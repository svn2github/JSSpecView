package jspecview.android.activity;

//TODO: 
// fix issue with reloading after orientation change
// fix zooming for spectrum with a lot of points

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


import jspecview.android.R;
import jspecview.android.PreferencesManager;
import jspecview.common.Coordinate;
import jspecview.common.IntegralData;
import jspecview.common.JDXSpectrum;
import jspecview.common.Parameters;
import jspecview.exception.JSpecViewException;
import jspecview.source.FileReader;
import jspecview.source.JDXSource;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.Point;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.lamerman.FileDialog;

import android.R.drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity{
	
	// Tag for logging
	static final String LOG_TAG = "SpectrumActivity";   
	
	// Preferences Manager
	PreferencesManager prefMan;
		    
    // Collection of Loaded JDXSpectrum objects
    List<JDXSpectrum> mSpectra;     
    // The current visible spectrum
    JDXSpectrum mCurrentSpectrum;   
    // The index of the current visible spectrum
    int mCurrentSpectrumIndex = 0;
    // series of all spectra loaded
    XYMultipleSeriesDataset[] mDatasets;
    // renderer for current spectra/series loaded
    XYMultipleSeriesRenderer mCurrentRenderer;
    // renderers for all spectra/series
    XYMultipleSeriesRenderer[] mRenderers;
    // the view for the current visible spectrum or overlaid spectra
    GraphicalView mCurrentView;
    // dialog for displaying the list of spectra in an n-tuple file
    AlertDialog mSelectSpectraDialog;
    // the path of the loaded spectrum/spectra
    String mSpectrumFilePath;    
    // current exception passed from the non-UI threads
    Exception mException;
    // determines whether the grid is visible
    boolean mIsGridVisible = true;
    // determines whether coordinates should be displayed when a point on the spectrum is touched
    boolean mIsCoordinatesEnabled = false;
    // determined if n-tuple spectra should be overlaid
    boolean mIsOverlayEnabled = false;
    // determines if a spectrum/spectra has finished loading
    boolean mIsLoadingComplete = false;
    // determines whether integration is enabled for the spectrum
    boolean mIsIntegrated = false;
    // integral graph for all valid spectra
    IntegralData[] mIntegralGraphs;
    // Holds the values of the current integration parameters
    // TODO: serialize the IntegralGraph
    double mIntegrationMinY = Double.MIN_VALUE;
    double mIntegrationOffset = Double.MIN_VALUE;
    double mIntegrationFactor = Double.MIN_VALUE;
        
    InputStream mSpectrumInputStream = null;
    
    // List of colors for spectrum plots. Each overlaid spectra will have colors in the array respectively. 
    // When colors are exhausted then a random color is generated    
    int[] colors = new int[] { Color.BLUE, Color.GREEN, Color.CYAN, Color.YELLOW, Color.RED, Color.MAGENTA, Color.WHITE};
     
    /*
     * Options Menu IDs
     */
    static class MenuItemID{
    	public static final int OPEN = 0;
    	public static final int REVERSE_PLOT = 1;
    	public static final int SHOW_HIDE_GRID = 2;
    	public static final int ENABLE_COOORDINATES = 3;
    	public static final int RESET = 4;
    	public static final int SELECT_SPECTRUM = 5;
    	public static final int OVERLAY_SPLIT = 6;
    	public static final int ADD_REMOVE_INTEGRATION = 7;
    	public static final int PREFERENCES = 8;
    }
	
	/*
	 * Keys used to save the state of the activity in a Bundle
	 */
	static class StateKey{
		public static final String SPECTRUM_FILE_PATH = "SpectrumFilePath";
		public static final String GRID_VISIBLE = "GridVisible";
		public static final String COORDINATES_ENABLED = "CoordinatesEnabled";
		public static final String OVERLAY_ENABLED = "OverlayEnabled";
		public static final String IS_INTEGRATED = "IsIntegrated";
		public static final String CURRENT_SPECTRUM_INDEX = "CurrentSpectrumIndex";
		public static final String INTEGRATION_PARAMS = "IntegrationParameters";
	}

	
	// ID for the integration dialog
	static final int DIALOG_INTEGRATION = 0;
	//Integration Dialog
	Dialog mIntegrateDialog;
    
    // Handler for callbacks to the UI Thread
    final Handler mHandler = new Handler();
    
	 // Runnable for creating a GraphicalView for displaying the spectra/spectrum and  posting to UI Thread
    final Runnable mCreateSpectraView = new Runnable() {		
		public void run() {		
			
			
			// Empty the layout in case it already contains a spectrum
			LinearLayout layout = getLayout();			
			layout.removeAllViews();
			
			// Check if the worker thread threw an exception
			if(mException != null){
				Toast.makeText(getApplicationContext(), mException.getMessage(), Toast.LENGTH_SHORT).show();
				mException = null;
				return;
			}
			
			int numDatasets = mDatasets.length;
						
			mRenderers = new XYMultipleSeriesRenderer[numDatasets];
			mIntegralGraphs = new IntegralData[numDatasets];
			
			// Create a graphical view for each dataset in the mDatasets array
			// For a Single spectrum, the array will have only one dataset with a single series
			// For multiple spectra, if overlay is enabled then the array will have one dataset with multiple series
			// If overlay is disabled then the array will have a dataset with a single series for each spectrum
			for(int i = 0; i < numDatasets; i++){
				XYMultipleSeriesDataset dataset = mDatasets[i];
				XYMultipleSeriesRenderer multiRenderer = createMultipleSeriesRenderer(dataset.getSeriesCount());
				mRenderers[i] = multiRenderer;
								
				multiRenderer.setXAxisDecreasing(!mSpectra.get(i).shouldDisplayXAxisIncreasing());
				
				GraphicalView view = ChartFactory.getLineChartView(getApplicationContext(), dataset, multiRenderer);
				view.setId(i);	
				//for multiple non-overlaid spectra set all but the first view to be invisible
				if(i != mCurrentSpectrumIndex){
					view.setVisibility(View.GONE);
				}
				else{
					// set the current spectrum, view and renderer to be for the first spectrum
					mCurrentRenderer = multiRenderer;
					mCurrentView = view;					
					mCurrentSpectrum = mSpectra.get(mCurrentSpectrumIndex);
					
					if(mIsIntegrated){
						addIntegrationSeries(mCurrentSpectrumIndex, dataset, multiRenderer, mIntegrationMinY, mIntegrationOffset, mIntegrationFactor);
					}
				}
				
				layout.addView(view);  
				LayoutParams params = view.getLayoutParams();
			    params.width = LayoutParams.FILL_PARENT;
			    params.height = LayoutParams.FILL_PARENT;
			    
			    setViewOnTouchListener(view);	
			    
			    mIsLoadingComplete = true;
			}		    		    		   
		}
	};
	

	 /**
     * Called when the activity is starting.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.main);
         
        if(savedInstanceState != null){
        	mSpectrumFilePath = savedInstanceState.getString(StateKey.SPECTRUM_FILE_PATH);
        	mIsGridVisible =  savedInstanceState.getBoolean(StateKey.GRID_VISIBLE);
        	mIsCoordinatesEnabled =  savedInstanceState.getBoolean(StateKey.COORDINATES_ENABLED);
        	mIsOverlayEnabled =  savedInstanceState.getBoolean(StateKey.OVERLAY_ENABLED);
        	mCurrentSpectrumIndex = savedInstanceState.getInt(StateKey.CURRENT_SPECTRUM_INDEX);        	
        	mIsIntegrated = savedInstanceState.getBoolean(StateKey.IS_INTEGRATED);
        	if(mIsIntegrated){
	        	double[] integrationParams = savedInstanceState.getDoubleArray(StateKey.INTEGRATION_PARAMS);
	        	mIntegrationMinY = integrationParams[0];
	        	mIntegrationFactor = integrationParams[1];
	        	mIntegrationOffset = integrationParams[2];
        	}
        	
        	InputStream is = createFileInputStream();
        	if(is != null){
        		loadSpectrum(is);
        	}      	
        }
                
                      
        PreferenceManager.setDefaultValues(this, R.xml.preference, false);
        prefMan = new PreferencesManager(this);
    }

	private InputStream createFileInputStream() {
		File file = new File(mSpectrumFilePath);
		InputStream is = null;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			Toast.makeText(getApplicationContext(), 
					String.format("File '{0}' not found", mSpectrumFilePath), 
					Toast.LENGTH_SHORT).show();
		}
		
		return is;
	}
    
    @Override
    protected void onStop() {    	
    	super.onStop();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putString(StateKey.SPECTRUM_FILE_PATH, mSpectrumFilePath);
    	outState.putBoolean(StateKey.GRID_VISIBLE, mIsGridVisible);
    	outState.putBoolean(StateKey.COORDINATES_ENABLED, mIsCoordinatesEnabled);
    	outState.putBoolean(StateKey.OVERLAY_ENABLED, mIsOverlayEnabled);
    	outState.putBoolean(StateKey.IS_INTEGRATED, mIsIntegrated);
    	outState.putInt(StateKey.CURRENT_SPECTRUM_INDEX, mCurrentSpectrumIndex);
    	if(mIsIntegrated){
    		IntegralData ig = mIntegralGraphs[mCurrentSpectrumIndex];    		
    		outState.putDoubleArray(StateKey.INTEGRATION_PARAMS, new double[]{ig.getPercentMinimumY(), ig.getIntegralFactor(), ig.getPercentOffset()});
    	}
    };
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig){
    	super.onConfigurationChanged(newConfig);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	MenuItem miOpen = menu.add(0, MenuItemID.OPEN, 0, "Open");
    	{
    		miOpen.setAlphabeticShortcut('o');
    		miOpen.setIcon(drawable.ic_menu_add);
    	}
    	MenuItem miPreferences = menu.add(0, MenuItemID.PREFERENCES, 0, "Preferences");
    	{
    		miPreferences.setAlphabeticShortcut('p');
    		miPreferences.setIcon(drawable.ic_menu_preferences);
    	}
    	    	
    	return true;
    };
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	
    	if(mIsLoadingComplete){
	    	
    		// if menu items are not created yet
    		if(menu.findItem(MenuItemID.REVERSE_PLOT) == null){
    		
	    		MenuItem miReversePlot = menu.add(0, MenuItemID.REVERSE_PLOT, 1, "Reverse Plot");
		    	{
		    		miReversePlot.setAlphabeticShortcut('r');
		    	}
		    	MenuItem miShowHideGrid = menu.add(0, MenuItemID.SHOW_HIDE_GRID, 5, mIsGridVisible ? "Hide Grid" : "Show Grid");
		    	{    		
		    		miShowHideGrid.setAlphabeticShortcut('g');
		    	}
		    	MenuItem miEnableCoordinates = menu.add(0, MenuItemID.ENABLE_COOORDINATES, 6, mIsCoordinatesEnabled ? "Disable Coordinates" : "Enable Coordinates");
		    	{    		
		    		miEnableCoordinates.setAlphabeticShortcut('c');
		    	}
		    	MenuItem miReset = menu.add(0, MenuItemID.RESET, 4, "Reset");
		    	{    		
		    		miReset.setAlphabeticShortcut('e');
		    	}
    		}
	    	if(canOverlay()){
	    		MenuItem miOverlaySplitSpectra = menu.findItem(MenuItemID.OVERLAY_SPLIT);
	    		String overlaySplitTitle = mIsOverlayEnabled ? "Split Spectra" : "Overlay Spectra";
    			if(miOverlaySplitSpectra == null){    					
    				miOverlaySplitSpectra = menu.add(0, MenuItemID.OVERLAY_SPLIT, 3, overlaySplitTitle);
    		    	{    		
    		    		miOverlaySplitSpectra.setAlphabeticShortcut('v');
    		    	}
    			}
    			else{
    				miOverlaySplitSpectra.setTitle(overlaySplitTitle);
    			}
	    		
    			MenuItem miSelectSpectrum = menu.findItem(MenuItemID.SELECT_SPECTRUM);
    			if(mIsOverlayEnabled){    				
	    			if(miSelectSpectrum != null){
	    				miSelectSpectrum.setVisible(false).setEnabled(false);	    						
	    			}	    					    		
    			}	 
    			else{
    				if(miSelectSpectrum != null){
    					miSelectSpectrum.setVisible(true).setEnabled(true);
    				}
    				else{
	    				miSelectSpectrum = menu.add(0, MenuItemID.SELECT_SPECTRUM, 2, "Select Spectrum");
				    	{    		
				    		miSelectSpectrum.setAlphabeticShortcut('s');
				    	}
    				}
    			}
	    	}
	    	if(!mIsOverlayEnabled && mSpectra.get(0).isHNMR()){
	    		MenuItem miIntegrateSpectra = menu.findItem(MenuItemID.ADD_REMOVE_INTEGRATION);	    		
	    		String integrationTitle = mIsIntegrated ? "Remove Integration" : "Integrate";
    			if(miIntegrateSpectra == null){    					
    				miIntegrateSpectra = menu.add(0, MenuItemID.ADD_REMOVE_INTEGRATION, 3, integrationTitle);
    		    	{    		
    		    		miIntegrateSpectra.setAlphabeticShortcut('i');
    		    	}
    			}
    			else{
    				miIntegrateSpectra.setTitle(integrationTitle);
    				miIntegrateSpectra.setEnabled(true);
    			}
	    	}
    	}
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()){
    		case MenuItemID.OPEN:
    			mIsLoadingComplete = false;    			
    			Intent intent = new Intent(this.getBaseContext(),FileDialog.class);
    			intent.putExtra(FileDialog.START_PATH, prefMan.isRememberLastDirectoryEnabled() ? prefMan.getLastDirectory() : PreferencesManager.START_DIR);
    			this.startActivityForResult(intent, 1);
    			return true;  
    		case MenuItemID.REVERSE_PLOT:
    			mCurrentRenderer.setXAxisDecreasing(!mCurrentRenderer.isXAxisDecreasing());
    			mCurrentView.repaint();
    			return true;
    		case MenuItemID.SHOW_HIDE_GRID:
    			mIsGridVisible = !mIsGridVisible;
    			mCurrentRenderer.setShowGrid(mIsGridVisible);    			
    			item.setTitle(mIsGridVisible ? "Hide Grid" : "Show Grid");    			
    			mCurrentView.repaint();
    			return true;
    		case MenuItemID.RESET:
    			mCurrentView.zoomReset();
    			return true;
    		case MenuItemID.ENABLE_COOORDINATES:
    			mIsCoordinatesEnabled = !mIsCoordinatesEnabled;
    			item.setTitle(mIsCoordinatesEnabled ? "Disable Coordinates" : "Enable Coordinates"); 
    			return true;
    		case MenuItemID.SELECT_SPECTRUM:
    			showSelectSpectrumDialog();
    			return true;
    		case MenuItemID.OVERLAY_SPLIT:
    			mIsOverlayEnabled = !mIsOverlayEnabled;
        		item.setTitle(mIsOverlayEnabled ? "Split Spectra" : "Overlay Spectra");    
        		reloadSpectrum();
    			return true;
    		case MenuItemID.ADD_REMOVE_INTEGRATION:
    			mIsIntegrated = !mIsIntegrated;    			
    			if(mIsIntegrated){ 
    				item.setEnabled(false);
    				showDialog(DIALOG_INTEGRATION);   	
    			}
    			else{
    				removeIntegrationFromView();
    				item.setTitle("Integrate");
    			}   
    			return true;
    		case MenuItemID.PREFERENCES:
    			Intent prefIntent = new Intent(this, MainPreferenceActivity.class); 
                startActivity(prefIntent);
    			return true;
    	}
    	return false;
    }
        
    /*
     * handles the result of the file open dialog to load a spectrum file
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == Activity.RESULT_OK) {
    		
    		LinearLayout layout = getLayout();			
			layout.removeAllViews();
			addLoadingMessage(layout);  
    		
            final String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
            mSelectSpectraDialog = null;
            mSpectrumFilePath = filePath;
            
            InputStream is = createFileInputStream();
        	if(is != null){
        		loadSpectrum(is);
        	}            
            
            // get the directory of the file and set the Last Directory preference
            String dirPath = filePath.substring(0, filePath.lastIndexOf("/") + 1);            
            prefMan.setLastDirectory(dirPath);
            
            mIsIntegrated = false;
            
    	} else if (resultCode == Activity.RESULT_CANCELED) {
           
    	}
    }
    
    @Override
    protected Dialog onCreateDialog(int id) 
    {
    	Dialog dialog;
    	switch(id){
    	case DIALOG_INTEGRATION:
    		createIntegrateDialog();
    		dialog = mIntegrateDialog;
    		break;
    	default:
            dialog = null; break;
    	}
    	return dialog;
    };
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) 
    {
    	switch(id){
    	case DIALOG_INTEGRATION:
    		initializeIntegrationDialog(dialog);
    	break;
    	}    	
    };
    
    /*
     * Shows the dialog to select a spectrum to view when an n-tuple spectra file is loaded and overlay is disabled
     */
    private void showSelectSpectrumDialog(){
  	
    	if(mSelectSpectraDialog ==  null){
	    	int numSpectra = mSpectra.size();
	    	final CharSequence[] items = new CharSequence[numSpectra];
	    	for(int i = 0; i < items.length; i++){
	    		items[i] = mSpectra.get(i).getTitle();
	    	}
	    	
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setTitle("Overlay Options");    	
	    	
	    	builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int itemId) {   	    	
		    		LinearLayout layout = getLayout();
		    		GraphicalView view = (GraphicalView)layout.findViewById(itemId);
		    		mCurrentView.setVisibility(View.GONE);
		    		view.setVisibility(View.VISIBLE);
		    		mCurrentView = view;
		    		mCurrentRenderer = mRenderers[itemId];
		    		mCurrentSpectrum = mSpectra.get(itemId);
		    		mCurrentSpectrumIndex = itemId;
		    		dialog.dismiss();
	    	    }
	    	});
	    	mSelectSpectraDialog = builder.create();
    	}
    	mSelectSpectraDialog.show();
    }
    
    private void createIntegrateDialog(){
    	mIntegrateDialog = new Dialog(this);
    	mIntegrateDialog.setContentView(R.layout.integrate_dialog);
    	mIntegrateDialog.setTitle("Integration Parameters");
    	
    	//initializeIntegrationDialog();
    	 	
    	Button okButton = (Button)mIntegrateDialog.findViewById(R.id.okButton);
    	okButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {	
				
				EditText minimumYEditText = (EditText)mIntegrateDialog.findViewById(R.id.minimumYEditText);
		    	EditText integralOffsetEditText = (EditText)mIntegrateDialog.findViewById(R.id.integralOffsetEditText);
		    	EditText integralFactorEditText = (EditText)mIntegrateDialog.findViewById(R.id.integralFactorEditText);	
				
				
				String minY = minimumYEditText.getText().toString();
				String factor = integralFactorEditText.getText().toString();
				String offset =  integralOffsetEditText.getText().toString();
				
				if(minY == "" && factor == "" && offset == ""){
					Toast.makeText(getApplicationContext(), "Please supply integration parameters", Toast.LENGTH_SHORT).show();					
				}
				else{
					mIntegrateDialog.dismiss();
					addIntegrationToView(Double.parseDouble(minY), Double.parseDouble(offset), Double.parseDouble(factor));					
				}				
			}
    	});
    	
    }

	private void initializeIntegrationDialog(Dialog dialog) {
		EditText minimumYEditText = (EditText)dialog.findViewById(R.id.minimumYEditText);
    	EditText integralOffsetEditText = (EditText)dialog.findViewById(R.id.integralOffsetEditText);
    	EditText integralFactorEditText = (EditText)dialog.findViewById(R.id.integralFactorEditText);
    	  
    	
		//IntegralGraph integGraph = mIntegralGraphs[mCurrentSpectrumIndex];    	
    	
    	if(mIntegrationMinY != Double.MIN_VALUE){
    		minimumYEditText.setText(String.valueOf(mIntegrationMinY));
    		integralFactorEditText.setText(String.valueOf(mIntegrationFactor));
    		integralOffsetEditText.setText(String.valueOf(mIntegrationOffset));    		
    	}    	
    	else{
    		minimumYEditText.setText(String.valueOf(IntegralData.DEFAULT_MINY));
    		integralFactorEditText.setText(String.valueOf(IntegralData.DEFAULT_RANGE));
    		integralOffsetEditText.setText(String.valueOf(IntegralData.DEFAULT_OFFSET)); 
    	}	
    	
    	mIntegrateDialog = dialog;
	}

	private void addLoadingMessage(LinearLayout layout) {
		TextView loadingTextView = new TextView(this);
		loadingTextView.append("Loading spectra. Please wait ... ");
		layout.addView(loadingTextView);
	}
    
	/*
	 * Creates a new thread to load a spectrum from a given file path and post the spectrum view to the UI
 	 *
	 */
	private void loadSpectrum(final InputStream is) {				
		Thread thread = new Thread(new Runnable() {			
			public void run() {					
				try {					  					
					mSpectrumInputStream = is; 
					mSpectra = readSpectrum(mSpectrumInputStream);												
					mDatasets = createDatasets(mSpectra);
					mHandler.post(mCreateSpectraView);
				} catch (Exception e) {					
					mException = e;
				}	
			}	
		});
		thread.start();		
	}
	    
	/*
	 * Reloads a spectrum from the JDXSpectrum object
	 */
    private Thread reloadSpectrum(){
    	mIsLoadingComplete = false;
    	Thread thread = new Thread(new Runnable() {			
			public void run() {				
				try{								       														
					mDatasets = createDatasets(mSpectra);	    
				}
				catch(Exception e){
					mException = e;
				}
				finally{
					mHandler.post(mCreateSpectraView);
				}
			}
		});
        thread.start();
        
        return thread;
    }
    
    /**
     * Reads a spectrum from an input stream
     * @param stream
     * @return
     * @throws IOException
     */
	private List<JDXSpectrum> readSpectrum(InputStream stream) throws IOException, JSpecViewException {		                
		JDXSource source = FileReader.createJDXSource(stream, false);             	
    	JDXSource jdxSource = (JDXSource)source;
    	List<JDXSpectrum> jdxSpectra = jdxSource.getSpectra();        	
    	return jdxSpectra;   
	}
    
  
    /*
     * Creates an array of XYMultipleSeriesDataset from a list of spectra
     * The array is created based on the following criteria:
     * For a Single spectra, the array will have only one dataset with a single series
	 * For multiple spectra, if overlay is enabled then the array will have one dataset with multiple series
	 * If overlay is disabled then the array will have a dataset with a single series for each spectrum
     */
    private XYMultipleSeriesDataset[] createDatasets(List<JDXSpectrum> spectra) {   	
    	    	    	
    	int numSpectra = spectra.size();
    	XYMultipleSeriesDataset[] datasets; 
    	boolean shouldOverlay = shouldOverlay();
       	
    	datasets = shouldOverlay? new XYMultipleSeriesDataset[1] : new XYMultipleSeriesDataset[numSpectra];
        	
    	XYMultipleSeriesDataset overlayDataset = new XYMultipleSeriesDataset();
    	
    	for(int j = 0; j < numSpectra; j++){
    		JDXSpectrum spectrum = spectra.get(j);
    		Coordinate[] xyCoords = spectrum.getXYCoords();  	
    		XYSeries series = new XYSeries(spectrum.getTitle());
    		     	
	    	for(int i = 0; i < xyCoords.length; i++){
	    		Coordinate coord = xyCoords[i]; 
	    		series.add(coord.getXVal(), coord.getYVal());
	    	}
			 
	    	if(shouldOverlay){
	    		overlayDataset.addSeries(series);
	    		if(j == numSpectra - 1){
	    			datasets[0] = overlayDataset;
	    		}
	    	}
	    	else{
	    		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		    	dataset.addSeries(series);
		    	datasets[j] = dataset;
	    	}
    	}
    	    	    	    	
    	return datasets;
    }
    

    /*
     * Adds an integration graph to the current HNMR spectrum 
     */
    private void addIntegrationToView(double minY, double offset, double factor){
    
		try {	
			XYMultipleSeriesDataset dataset = mDatasets[mCurrentSpectrumIndex];		
			XYMultipleSeriesRenderer multiRenderer = mRenderers[mCurrentSpectrumIndex];
			
			addIntegrationSeries(mCurrentSpectrumIndex, dataset, multiRenderer, minY, offset, factor);
			
			GraphicalView view = ChartFactory.getLineChartView(getApplicationContext(), dataset, multiRenderer);
												
			LinearLayout layout = getLayout();	
			int id = mCurrentView.getId();
			layout.removeView(mCurrentView);
								
			view.setId(id);
			layout.addView(view, mCurrentSpectrumIndex);  
			LayoutParams params = view.getLayoutParams();
		    params.width = LayoutParams.FILL_PARENT;
		    params.height = LayoutParams.FILL_PARENT;
		    
		    mCurrentRenderer = multiRenderer;
			mCurrentView = view;	
		    
		    setViewOnTouchListener(view);	    		   
				
		}
		catch (Exception ex) {	
			Toast.makeText(getApplicationContext(), "Error adding integration", Toast.LENGTH_SHORT).show();
		}
    }
    
    /*
     * Adds the integration graph series to the current dataset
     */
    private void addIntegrationSeries(int spectrumIndex, XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer multiRenderer,
    		double minY, double offset, double factor){
    	IntegralData integGraph = mIntegralGraphs[spectrumIndex];
    	JDXSpectrum spectrum = mSpectra.get(spectrumIndex);
		
		if(integGraph == null){
		  Parameters params = new Parameters("android");
		  params.integralMinY = minY;
		  params.integralOffset = offset;
		  params.integralRange = factor;
			integGraph = new IntegralData(minY, offset, factor, spectrum);
			
			mIntegralGraphs[spectrumIndex] = integGraph;
		}
	
		XYSeries series = new XYSeries("Integral - " + spectrum.getTitle());
		Coordinate[] integCoords = integGraph.getXYCoords();
		for(int i = 0; i < integCoords.length; i++){
			series.add(integCoords[i].getXVal(), integCoords[i].getYVal());
		}
		dataset.addSeries(series);				
		
		multiRenderer.addSeriesRenderer(createXYSeriesRenderer(prefMan.getIntegralPlotColor()));    	
    }
    
    /*
     * Removes and integration graph from the spectrum
     */
    private void removeIntegrationFromView(){
    
		try {							
			XYMultipleSeriesDataset dataset = mDatasets[mCurrentSpectrumIndex];			
			dataset.removeSeries(1);				
			
			XYMultipleSeriesRenderer multiRenderer = mRenderers[mCurrentSpectrumIndex];
			multiRenderer.removeSeriesRenderer(multiRenderer.getSeriesRendererAt(1));
							
			GraphicalView view = ChartFactory.getLineChartView(getApplicationContext(), dataset, multiRenderer);
												
			LinearLayout layout = getLayout();	
			int id = mCurrentView.getId();
			layout.removeView(mCurrentView);
								
			view.setId(id);
			layout.addView(view, mCurrentSpectrumIndex);  
			LayoutParams params = view.getLayoutParams();
		    params.width = LayoutParams.FILL_PARENT;
		    params.height = LayoutParams.FILL_PARENT;
		    
		    mCurrentRenderer = multiRenderer;
			mCurrentView = view;	
		    
		    setViewOnTouchListener(view);	    		   
		}
		catch (Exception ex) {	
			Toast.makeText(getApplicationContext(), "Error removing integration", Toast.LENGTH_SHORT).show();
		}
    }
    
    /*
     * Determines if a spectrum file should be overlaid
     */
	private boolean shouldOverlay() {
		return canOverlay() && mIsOverlayEnabled;
	}
	
	/*
	 * Determines if a spectrum file can be overlaid
	 */
	private boolean canOverlay(){
		return mSpectra.size() > 1;
	}
    
	/*
	 * Generates a random color
	 */
    private int generateRandomColor(){
	    int red = (int)(Math.random() * 255);
	    int green = (int)(Math.random() * 255);
	    int blue = (int)(Math.random() * 255);
	
	    int randomColor = Color.rgb(red, green, blue);
	    
	    return randomColor;
	 }

    /*
     * Get the layout where the spectrum GraphicalView should be added
     */
	private LinearLayout getLayout() {
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		return layout;
	}
	
	/*
	 * Creates a multiple series renderer given a parameter determining how many series to create
	 */
	private XYMultipleSeriesRenderer createMultipleSeriesRenderer(int numSeries) {
		
		XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
		
		multiRenderer.setApplyBackgroundColor(true);
		multiRenderer.setBackgroundColor(Color.WHITE);		    
		multiRenderer.setGridColor(Color.GRAY);
		multiRenderer.setYLabelsAlign(Align.RIGHT);
		multiRenderer.setAxisTitleTextSize(16);
		multiRenderer.setChartTitleTextSize(20);
		multiRenderer.setLabelsTextSize(15);
		multiRenderer.setLegendTextSize(15);
		multiRenderer.setMargins(new int[] { 0, 40, 0, 0 });
		multiRenderer.setZoomEnabled(true, true);
		multiRenderer.setExternalZoomEnabled(true);
		multiRenderer.setPanLimits(null);
		multiRenderer.setZoomLimits(null);
		multiRenderer.setShowGrid(mIsGridVisible);
		multiRenderer.setXLabels(10);
					
		int numColors = colors.length;		    
	    for(int i =0; i < numSeries; i++){
	    	XYSeriesRenderer renderer = null;
	    	if(i < numColors){
	    		renderer = createXYSeriesRenderer(colors[i]);                                                 
			}
			else{
				renderer = createXYSeriesRenderer(generateRandomColor());
			}	    	
	    	multiRenderer.addSeriesRenderer(renderer);
	    }
	    		    
		return multiRenderer;
	}

	private XYSeriesRenderer createXYSeriesRenderer(int color) {
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		
		renderer.setPointStyle(PointStyle.POINT);
		renderer.setColor(color);                                                  
		
		return renderer;
	}

	private void setViewOnTouchListener(GraphicalView view) {
		// Set touch handler to show coordinates if coordinates in enabled
		view.setOnTouchListener(new OnTouchListener() {			    	
			@Override
			public boolean onTouch(View view, MotionEvent event) {
									
				if(event.getAction() == MotionEvent.ACTION_DOWN){
		    		if(mIsCoordinatesEnabled){
		    			float x = event.getX();
		    			float y = event.getY();
			    		
		    			GraphicalView gView = (GraphicalView)view;
			    		SeriesSelection selection = gView.getChart().getSeriesAndPointForScreenCoordinate(new Point(x, y));
			    		
			    		if(selection != null){
				    		double xVal = selection.getXValue();
				    		double yVal = selection.getValue();
				    		
				    		Toast toast = Toast.makeText(getApplicationContext(), String.format("%1$.0f, %2$.3f", xVal, yVal), Toast.LENGTH_SHORT);
				    		toast.show();
			    		}				    		
		    		}			    		
		    	}
		    	return false;
			}
		});
	}
}