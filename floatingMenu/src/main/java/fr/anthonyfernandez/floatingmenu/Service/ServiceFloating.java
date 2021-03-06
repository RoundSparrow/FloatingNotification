package fr.anthonyfernandez.floatingmenu.Service;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListPopupWindow;

import fr.anthonyfernandez.floatingmenu.R;
import fr.anthonyfernandez.floatingmenu.Adapter.CustomAdapter;
import fr.anthonyfernandez.floatingmenu.Manager.PInfo;
import fr.anthonyfernandez.floatingmenu.Manager.RetrievePackages;

public class ServiceFloating extends Service {

	public static  int ID_NOTIFICATION = 2018;

	private WindowManager windowManager;
	private ImageView menuActivationFloater;
	private ListPopupWindow popupAppListWindow;

	boolean mHasDoubleClicked = false;
	long lastPressTime;
	private Boolean _enable = true;

	ArrayList<String> appNameArrayList;
	ArrayList<PInfo> appProcessInfoArrayList;
	List appList;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		RetrievePackages getInstalledPackages = new RetrievePackages(getApplicationContext());
		appProcessInfoArrayList = getInstalledPackages.getInstalledApps(false);
		appNameArrayList = new ArrayList<String>();

		for(int i=0 ; i< appProcessInfoArrayList.size() ; ++i) {
			appNameArrayList.add(appProcessInfoArrayList.get(i).appname);
		}

		appList = new ArrayList();
		for(int i=0 ; i< appProcessInfoArrayList.size() ; ++i) {
			appList.add(appProcessInfoArrayList.get(i));
		}

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		menuActivationFloater = new ImageView(this);

		menuActivationFloater.setImageResource(R.drawable.floating2);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String menuActivationIconChoice = prefs.getString("ICON", "floating2");

		if(menuActivationIconChoice.equals("floating3")){
			menuActivationFloater.setImageResource(R.drawable.floating3);
		} else if(menuActivationIconChoice.equals("floating4")){
			menuActivationFloater.setImageResource(R.drawable.floating4);
		} else if(menuActivationIconChoice.equals("floating5")){
			menuActivationFloater.setImageResource(R.drawable.floating5);
		} else if(menuActivationIconChoice.equals("floating5")){
			menuActivationFloater.setImageResource(R.drawable.floating2);
		}

        int menuActivationIconColorFilter = prefs.getInt("ICONCOLOR", 0);
        // Var should only be in preferences if the user adjusted it
        if (prefs.contains("ICONCOLOR"))
        {
            Drawable mDrawable = menuActivationFloater.getDrawable();
            mDrawable.setColorFilter(new
                    PorterDuffColorFilter(menuActivationIconColorFilter, PorterDuff.Mode.MULTIPLY));
            menuActivationFloater.setImageDrawable(mDrawable);
        }

		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);

        // ToDo: Use Gravity.start to account for right and left oriented screens
		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = 0;
		params.y = 100;

		windowManager.addView(menuActivationFloater, params);

		try {
			menuActivationFloater.setOnTouchListener(new View.OnTouchListener() {
                private WindowManager.LayoutParams paramsF = params;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            // Get current time in nano seconds.
                            long pressTime = System.currentTimeMillis();

                            // If double click...
                            if (pressTime - lastPressTime <= 300) {
                                createNotification();
                                ServiceFloating.this.stopSelf();
                                mHasDoubleClicked = true;
                            } else {     // If not double click....
                                mHasDoubleClicked = false;
                            }
                            lastPressTime = pressTime;
                            initialX = paramsF.x;
                            initialY = paramsF.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            break;
                        case MotionEvent.ACTION_MOVE:
                            paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                            paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(menuActivationFloater, paramsF);
                            break;
                    }
                    return false;
                }
            });
		} catch (Exception e) {
			// TODO: handle exception
		}

		menuActivationFloater.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                initiatePopupWindow(menuActivationFloater);
                _enable = false;
                //				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                //				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                //				getApplicationContext().startActivity(intent);
            }
        });
	}


	private void initiatePopupWindow(View anchor) {
		try {
			Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			popupAppListWindow = new ListPopupWindow(this);
			popupAppListWindow.setAnchorView(anchor);
			popupAppListWindow.setWidth((int) (display.getWidth() / (1.5)));
			//ArrayAdapter<String> arrayAdapter =
			//new ArrayAdapter<String>(this,R.layout.list_item, myArray);
			popupAppListWindow.setAdapter(new CustomAdapter(getApplicationContext(), R.layout.row, appList));
			popupAppListWindow.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View view, int position, long id3) {
                    //Log.w("tag", "package : "+apps.get(position).pname.toString());
                    Intent i;
                    PackageManager manager = getPackageManager();
                    try {
                        i = manager.getLaunchIntentForPackage(appProcessInfoArrayList.get(position).pname.toString());
                        if (i == null)
                            throw new PackageManager.NameNotFoundException();
                        i.addCategory(Intent.CATEGORY_LAUNCHER);
                        startActivity(i);
                    } catch (PackageManager.NameNotFoundException e) {

                    }
                }
            });
			popupAppListWindow.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createNotification(){
		Intent notificationIntent = new Intent(getApplicationContext(), ServiceFloating.class);
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, notificationIntent, 0);

		Notification notification = new Notification(R.drawable.floating2, "Click to start launcher", System.currentTimeMillis());
		notification.setLatestEventInfo(getApplicationContext(), "Start launcher", "Click to start launcher", pendingIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT;

		NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(ID_NOTIFICATION,notification);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (menuActivationFloater != null) windowManager.removeView(menuActivationFloater);
	}
}