/* Copyright (c) 2002-2008 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;

import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;

/**
 * Class <code>JDXSourceFactory</code> creates an instance of a JDXSource from
 * an <code>InputStream</code>. It determines the type of source from the stream
 * and creates the instance of the corresponding class. Either <code>SimpleSource,
 * BlockSource or NTupleSource</code>
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 * @see jspecview.common.JDXSource
 */

public class JDXSourceFactory {

}
