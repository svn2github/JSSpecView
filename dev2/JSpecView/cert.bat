rem keytool -delete -alias JSVcertificate -keystore certificate\JSVcertificate.store -keypass JSV2012 -dname "cn=Chemistry Department UWI Jamaica" -storepass JSV2012

keytool -genkey -alias JSVcertificate -keystore certificate\JSVcertificate.store -keypass JSV2012 -dname "cn=Chemistry Department UWI Jamaica" -storepass JSV2012
