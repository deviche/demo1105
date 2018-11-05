package com.deviche.demo1105;

import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    public static final String TAG = MainActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        //Toast.makeText(this, "检查升级", Toast.LENGTH_SHORT).show();

        loadVersionInfo();
    }

    private void loadVersionInfo() {
        OkHttpClient client = new CustomHttpClient()
                .setConnectTimeout(5_000)
                .setShowLog(true)
                .createOkHttpClient();

        Retrofit retrofit = new CustomRetrofit()
                //我在app的build.gradle文件的defaultConfig标签里定义了BASE_URL
                .setBaseUrl("https://raw.githubusercontent.com/")
                .setOkHttpClient(client)
                .createRetrofit();
        Log.e(TAG, "loadVersionInfo:1111111111 " );
        retrofit.create(ApiService.class)
                .getVersionInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        //output.json文件里是JSONArrary, 我们取第一个JSONObject就好
                        s = s.substring(1, s.length() - 1);
                        VersionEntity entity = VersionEntity.fromJson(s);
                        Log.e(TAG, "onNext: 返回值 " +s );
                        showUpdateTipsDialog(entity);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "loadVersionInfo:222222222 " );
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "loadVersionInfo:33333333 " );
                    }
                });
                /*.subscribe(new LoadingDialogObserver<String>(createLoadingDialog()) {
                    @Override
                    public void onNext(String s) {
                        //output.json文件里是JSONArrary, 我们取第一个JSONObject就好
                        s = s.substring(1, s.length() - 1);
                        VersionEntity entity = VersionEntity.fromJson(s);

                        //showUpdateTipsDialog(entity);
                    }

                    @Override
                    public void onNetStart(Disposable disposable) {
                        Log.i("MainActivity", "onNetStart: ");
                    }

                    @Override
                    public void onNetError(Throwable e) {

                    }

                    @Override
                    public void onNetFinish(Disposable disposable) {

                    }
                });*/
    }


    private void showUpdateTipsDialog(final VersionEntity entity) {
        if (entity == null)
            return;

        int curVersionCode = 0;
        String curVersionName = "";
        try {
            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            curVersionCode = info.versionCode;
            curVersionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (curVersionCode > 0 && entity.getApkInfo().getVersionCode() > curVersionCode){
            new AlertDialog.Builder(this)
                    .setTitle("更新提示")
                    .setMessage("1、当前版本：" + curVersionName + "\n2、最新版本：" + entity.getApkInfo().getVersionName())
                    .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            // TODO: 2018/11/5
                            //checkPermissionBeforeDownloadApk(entity.getApkInfo().getVersionName());
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }else {
            Toast.makeText(this, "当前是最新版本", Toast.LENGTH_SHORT).show();
        }

    }


   /* private void checkPermissionBeforeDownloadApk(final String versionName){
        checkPermissions(0, new CustomPermissionChecker.OnCheckListener() {
            @Override
            public void onResult(int requestCode, boolean isAllGranted, @NonNull List<String> grantedPermissions, @Nullable List<String> deniedPermissions, @Nullable List<String> shouldShowPermissions) {
                if (isAllGranted){
                    downloadApk(versionName);
                    return;
                }

                if (shouldShowPermissions != null && shouldShowPermissions.size() > 0){
                    String message = "当前应用需要以下权限:\n\n" + getAllPermissionDes(shouldShowPermissions);
                    showPermissionRationaleDialog("温馨提示", message, "设置", "知道了");
                }
            }

            @Override
            public void onFinally(int requestCode) {
                recyclePermissionChecker();
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public void downloadApk(String versionName){
        registerDownloadCompleteReceiver();
        DownloadEntity entity = new DownloadEntity();
        entity.setUrl("https://raw.githubusercontent.com/JustinRoom/JSCKit/master/capture/JSCKitDemo.apk");
        entity.setSubPath("JSCKitDemo"+ versionName + ".apk");
        entity.setTitle("JSCKitDemo"+ versionName + ".apk");
        entity.setDesc("JSCKit Library");
        entity.setMimeType("application/vnd.android.package-archive");
        downloadFile(entity);
    }

    public final long downloadFile(DownloadEntity downloadEntity) {
        String url = downloadEntity.getUrl();
        if (TextUtils.isEmpty(url))
            return -1;

        Uri uri = Uri.parse(url);
        String subPath = downloadEntity.getSubPath();
        if (subPath == null || subPath.trim().length() == 0) {
            subPath = uri.getLastPathSegment();
        }

        File destinationDirectory = downloadEntity.getDestinationDirectory();
        if (destinationDirectory == null) {
            destinationDirectory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        }

        File file = new File(destinationDirectory, subPath);
        File directory = file.getParentFile();
        if (!directory.exists()){//创建文件保存目录
            boolean result = directory.mkdirs();
            if (!result)
                Log.e("APermissionCheck", "Failed to make directories.");
        }

        if (file.exists()){
//            boolean result = file.delete();
//            if (!result)
//                Log.e("APermissionCheck", "Failed to delete file.");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        DownloadManager.Request request = new DownloadManager.Request(uri);
        //设置title
        request.setTitle(downloadEntity.getTitle());
        // 设置描述
        request.setDescription(downloadEntity.getDesc());
        // 完成后显示通知栏
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        //
        Uri destinationUri = Uri.withAppendedPath(Uri.fromFile(destinationDirectory), subPath);
//        Uri destinationUri = FileProviderCompat.getUriForFile(this, file);
        request.setDestinationUri(destinationUri);
//        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, subPath);
        request.setMimeType(downloadEntity.getMimeType());
        request.setVisibleInDownloadsUi(true);

        DownloadManager mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        return mDownloadManager == null ? -1 : mDownloadManager.enqueue(request);
    }

    *//**
     * 注册下载完成监听
     *//*
    private void registerDownloadCompleteReceiver(){
        if (downloadReceiver == null)
            downloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())){
                        unRegisterDownloadCompleteReceiver();
                        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                        findDownloadFileUri(downloadId);
                    }
                }
            };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, intentFilter);
    }

    *//**
     * 注销下载完成监听
     *//*
    private void unRegisterDownloadCompleteReceiver(){
        if (downloadReceiver != null){
            unregisterReceiver(downloadReceiver);
            downloadReceiver = null;
        }
    }
*/

}
