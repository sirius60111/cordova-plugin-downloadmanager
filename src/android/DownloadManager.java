package downloadmanager;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * This class echoes a string called from JavaScript.
 */
public class DownloadManager extends CordovaPlugin {
    private long mDownloadReference;

    private BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            cordova.getActivity().unregisterReceiver(this);

            long downloadCompletedId = intent.getLongExtra(
                    android.app.DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            if (mDownloadReference != downloadCompletedId) {
                return;
            }
            android.app.DownloadManager.Query query = new android.app.DownloadManager.Query();
            query.setFilterById(mDownloadReference);
            android.app.DownloadManager downloadManager = (android.app.DownloadManager) cordova.getActivity().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);            
            Cursor c = downloadManager.query(query);
            if (c.moveToFirst()) {
                int columnIndex = c.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS);
                if (android.app.DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                    String uriString = c.getString(c.getColumnIndex(android.app.DownloadManager.COLUMN_LOCAL_FILENAME));
                    promptInstall(Uri.parse("file://" + uriString));
                }
            }
        }
    };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("download")) {
            String url = args.getString(0);
            String destination = args.getString(1);
            String mimeType = args.getString(2);
            String title = args.getString(3);
            String description = args.getString(4);
            this.startDownload(url, destination, mimeType, title, description, callbackContext);
            return true;
        }
        return false;
    }

    private void startDownload(String url, String destination, String mimeType, String title, String description, CallbackContext callbackContext) {
        if (url != null && url.length() > 0) {
            android.app.DownloadManager downloadManager = (android.app.DownloadManager) cordova.getActivity().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);            
            Uri downloadUri = Uri.parse(url);
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(downloadUri);
            //Restrict the types of networks over which this download may proceed.
            request.setAllowedNetworkTypes(android.app.DownloadManager.Request.NETWORK_WIFI | android.app.DownloadManager.Request.NETWORK_MOBILE);
            //Set whether this download may proceed over a roaming connection.
            request.setAllowedOverRoaming(false);
            //Set the title of this download, to be displayed in notifications (if enabled).
            request.setMimeType(mimeType);
            request.setTitle(title);
            //Set a description of this download, to be displayed in notifications (if enabled)
            request.setDescription(description);
            //Set the local destination for the downloaded file to a path within the application's external files directory            
            request.setDestinationInExternalFilesDir(cordova.getActivity().getApplicationContext(), Environment.DIRECTORY_DOWNLOADS, destination);
            //Set visiblity after download is complete
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            mDownloadReference = downloadManager.enqueue(request);
            if ("application/vnd.android.package-archive".equals(mimeType)) {  // APK auto install
                cordova.getActivity().registerReceiver(mDownloadReceiver, new IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
            callbackContext.success(url);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void promptInstall(Uri data) {
        Intent promptInstall = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(data, "application/vnd.android.package-archive");
        promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        cordova.getActivity().startActivity(promptInstall);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cordova.getActivity().unregisterReceiver(mDownloadReceiver);
    }
}