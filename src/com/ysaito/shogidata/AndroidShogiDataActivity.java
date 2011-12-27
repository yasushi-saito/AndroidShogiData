package com.ysaito.shogidata;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidShogiDataActivity extends Activity {
	private File mExternalDir;
	private ZipExtractor mZipExtractor;
	private ProgressDialog mExtractStatusDialog;
	private String mErrorMessage;
	static final int DIALOG_FATAL_ERROR = 1234;
  static final int DIALOG_CONFIRM_EXTRACT = 1235;
  static final int DIALOG_EXTRACT_STATUS = 1236;
  
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mExternalDir = getShogiDataDir();
		if (mExternalDir == null) {
			FatalError("Please mount the sdcard on the device");
			return;
		}
		mZipExtractor = new ZipExtractor(mExtractHandler, mExternalDir);
		if (mZipExtractor.hasRequiredFiles()) {
			showDialog(DIALOG_CONFIRM_EXTRACT);
		} else {
			startExtract();
		}
	}

	private File getShogiDataDir() {
  	File sdDir = getExternalFilesDir(null);
  	if (sdDir == null) return null;  // sdcard not mounted

  	String path = sdDir.getAbsolutePath();
  	if (path.indexOf(".ysaito.shogidata") < 0) {
  		Toast.makeText(getBaseContext(),
  				"Wrong package name?? " + path,
  				Toast.LENGTH_LONG).show();
  		return null;
  	}
  	
  	// Create the shogi data files in the private application dir for
  	// the main shogi program.
  	String newPath = path.replace(".ysaito.shogidata", ".ysaito.shogi");
  	File f = new File(newPath);
  	f.mkdirs();
  	return f;
	}
	
	private void FatalError(String message) {
		mErrorMessage = message;
		showDialog(DIALOG_FATAL_ERROR);
	}

	private void startExtract() {
		AssetManager am = getResources().getAssets();
	  InputStream zip_in = null;
		try {
			zip_in = am.open("shogi-data.zip", AssetManager.ACCESS_STREAMING);
		} catch (IOException e) {
			FatalError("Failed to read shogi-data.zip in asset: " + e.toString());
			return;
		}
    showDialog(DIALOG_EXTRACT_STATUS);
		mZipExtractor.start(zip_in);
	}
	
	private void startShogi() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setComponent(new ComponentName("com.ysaito.shogi","com.ysaito.shogi.StartScreenActivity"));
		startActivity(intent);
	}
	
	@Override protected void onPrepareDialog(int id, Dialog d) {
		if (id == DIALOG_FATAL_ERROR) {
			((AlertDialog)d).setMessage(mErrorMessage);
		}
	}
	
  @Override protected Dialog onCreateDialog(int id) {
  	switch (id) {
  	case DIALOG_FATAL_ERROR: 
  		return newFatalErrorDialog();
  	case DIALOG_CONFIRM_EXTRACT: 
  		return newConfirmExtractDialog();
  	case DIALOG_EXTRACT_STATUS: 
  		return (mExtractStatusDialog = newExtractStatusDialog());
  	}
  	return null;
  }
  	
  private Dialog newFatalErrorDialog() {
  	DialogInterface.OnClickListener cb = new DialogInterface.OnClickListener() {
  		public void onClick(DialogInterface dialog, int id) {  };
  	};
  	AlertDialog.Builder builder = new AlertDialog.Builder(this);
  	// The dialog message will be set in onPrepareDialog
  	builder.setMessage("???") 
  	.setCancelable(false)
  	.setPositiveButton("Ok", cb);
  	return builder.create();
  }
  
	private Dialog newConfirmExtractDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Shogi database files already seem to exist. Overwrite? ")
		.setCancelable(true)
		.setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) { startExtract(); }
			  })
		.setNegativeButton("No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) { startShogi(); }
			  });
		return builder.create();
	}
	
	private ProgressDialog newExtractStatusDialog() {
		// TODO(saito) screen rotation will abort downloading.
		ProgressDialog d = new ProgressDialog(this);
		d.setCancelable(true);
		d.setMessage("Extracting Shogi database files");
		d.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface unused) {
				if (mZipExtractor != null) mZipExtractor.destroy();
			}
		});
		return d;
	}
	  
	private final ZipExtractor.EventListener mExtractHandler = new ZipExtractor.EventListener() {
		public void onProgressUpdate(String status) {
			if (mExtractStatusDialog != null) {
				mExtractStatusDialog.setMessage(status);
			}
		}
		
		public void onFinish(String status) {
			mExtractStatusDialog.dismiss();
			if (status != null) {
				FatalError(status);
				return;
			}
			startShogi();
		}
	};
}