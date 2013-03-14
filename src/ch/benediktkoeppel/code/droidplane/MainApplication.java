package ch.benediktkoeppel.code.droidplane;

import android.app.Application;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey = "dE1VQVpQN2FNTWlLQXg1UUQ1b1VSN3c6MQ", mode = ReportingInteractionMode.TOAST, resToastText = R.string.sending_crash_report) 
public class MainApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// initialize ACRA crash reports
		ACRA.init(this);
	}

}