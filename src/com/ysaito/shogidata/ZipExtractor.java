package com.ysaito.shogidata;

//Copyright 2010 Google Inc. All Rights Reserved.

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
* @author saito@google.com (Your Name Here)
*
* Helper class for downloading large data files (fv.bin etc) from the network.
* 
* Downloading runs in a separate thread. The thread will communicate its status
* through the EventListener.
*/public class ZipExtractor {
	public interface EventListener {
		/**
		 * Called multiple times to report progress.
		 * @param message Download status
		 */
		public void onProgressUpdate(String message);
		
		/**
		 *  Called exactly once when download finishes. error==null on success. Else, it contains an 
		 * @param error null on success. Contains an error message on error. 
		 */
		public void onFinish(String error);  
	}
 
	/**
	 * @param listener Used to report download status to the caller
	 * @param externalDir The directory to store the downloaded file.
	 * The basename of the file will be the same as the one in the sourceUrl.
	 */
	public ZipExtractor(EventListener listener,	File externalDir) {
		mListener = listener;
		mExternalDir = externalDir;
		mThread = new ExtractThread();
	}

	/**
	 * Must be called once to start downloading
	 * @param sourceUrl The location of the file.
	 */
	public void start(InputStream zip_in) {
		mThread.execute(zip_in);
	}

	/**
	 * Must be called to stop the download thread.
	 */
	public void destroy() {
		Log.d(TAG, "Destroy");
		mThread.cancel(false);
	}
	
	// 
	// Implementation details
	//
	private static final String TAG = "ShogiZipExtractor";
	private EventListener mListener;
	private File mExternalDir;
	private ExtractThread mThread;
	private String mError;

	private static void deleteFilesInDir(File dir) {
		String[] children = dir.list();
		if (children != null) {
			for (String child: children) {
				File f = new File(dir, child);
				if (!f.isDirectory()) {
					Log.d(TAG, "Deleting " + f.getAbsolutePath());
					f.delete();
				}
			}
		}
	}
	
 
  private class ExtractThread extends AsyncTask<InputStream, String, String> {
  	@Override protected String doInBackground(InputStream... stream) {
  		deleteFilesInDir(mExternalDir);
  		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(stream[0]));
  		try {
  			FileOutputStream out = null;
  			try {
  				ZipEntry ze = null;
  				while ((ze = zis.getNextEntry()) != null) {
  					Log.d(TAG, "Found zip entry:" + ze.toString());
  					publishProgress("Extracting " + ze.getName());
  					File outPath = new File(mExternalDir, ze.getName());
  					out = new FileOutputStream(outPath);
  					copyStream(ze.getName() + ": %d bytes extracted", zis, out);
  				}
  			} finally {
  				zis.close();
  				if (out != null) out.close();
  			}
  		} catch (IOException e) {
  			Log.e(TAG, "Exception: " + e.toString());
  			String msg = "Failed to extract file: " + e.toString(); 
  			if (e != null) msg += " for zip: " + e.toString();
  			return msg;
  		}
  		return null;
  	}

  	@Override public void onProgressUpdate(String... status) {
  		for (String s: status) mListener.onProgressUpdate(s);
    }
    
    @Override public void onPostExecute(String status) {
      mListener.onFinish(status);
    }
    
   private final byte[] mBuf = new byte[256 << 10];
   
   private void copyStream(String format, InputStream in, OutputStream out) throws IOException {
     long cumulative = 0;
     long lastReported = 0;
     int n;
     while ((n = in.read(mBuf)) > 0) {
       out.write(mBuf, 0, n);
       cumulative += n;
       if (cumulative - lastReported >= 256 * 1000) {
         publishProgress(String.format(format, cumulative));
         lastReported = cumulative;
       }
       if (isCancelled()) {
         throw new IOException("Extraction cancelled by user");
       }
     }
   }
 }

  /**
  * See if all the files required to run Bonanza are present in externalDir.
  */
  private static final String[] REQUIRED_FILES = {
  	"book.bin", "fv.bin", "hash.bin"
  };
  public boolean hasRequiredFiles() {
  	for (String basename: REQUIRED_FILES) {
  		File file = new File(mExternalDir, basename);
  		if (!file.exists()) {
  			Log.d(TAG, file.getAbsolutePath() + " not found");
  			return false;
  		}
  	}
  	return true;
  }
}


