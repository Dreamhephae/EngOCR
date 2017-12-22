/** * * ━━━━━━神兽出没━━━━━━
 * 　　　┏┓　　　┏┓
 * 　　┃　　　　　　　┃
 * 　　┃　　　━　　　┃
 * 　　┃　┳┛　┗┳　┃
 * 　　┃　　　　　　　┃
 * 　　┃　　　┻　　　┃
 * 　　┃　　　　　　　┃
 * 　　┗━┓　　　┏━┛Code is far away from bug with the animal protecting
 * 　　　　┃　　　┃ 神兽保佑,代码无bug
 * 　　　　┃　　　┃
 * 　　　　┃　　　┗━━━┓
 * 　　　　┃　　　　　　　┣┓
 * 　　　　┃　　　　　　　┏┛
 * 　　　　┗┓┓┏━┳┓┏┛
 * 　　　　　┃┫┫　┃┫┫
 * 　　　　　┗┻┛　┗┻┛
 * * ━━━━━━感觉萌萌哒━━━━━━ */

package cn.dreamhephae.engocr;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA = 1;
    private static final int REQUEST_CODE_GALLERY = 2;
    private static final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpeg");
    private static final String url = "http://192.168.43.239:8080/PostServer/PostServer";
    private int testcount = 1;
    private TextView textView;
    private Handler res_handler = null;
    private String content = null;

    private String cur_cam_file_path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageButton button_next = findViewById(R.id.button_next);
        ImageButton button_select = findViewById(R.id.button_select);
        textView = findViewById(R.id.textView);
        textView.setTextIsSelectable(true);  //设置文字可被选中
        textView.setSelectAllOnFocus(true);  //设置文字获取焦点后被全选
        testcount++;
        textView.setText(String.valueOf(testcount));
        res_handler = new Handler();

        takePhoto();

        //按钮监听，拍摄键是否按下
        button_next.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                takePhoto();
            }
        });


        //按钮监听，选择键是否按下
        button_select.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_GALLERY);
            }
        });
    }


    //调用系统相机
    private void takePhoto() {

//        // 检查sd card是否存在
//        if (!Environment.getExternalStorageState().equals(
//                Environment.MEDIA_MOUNTED)) {
//            Log.i("mylog", "sd card is not available/writable right now.");
//            return;
//        }

        Intent intent = new Intent();
        intent.setAction("android.media.action.IMAGE_CAPTURE");
        intent.addCategory("android.intent.category.DEFAULT");

        String imageFilePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
        //根据当前时间生成图片的名称
        String timestamp = "/"+formatter.format(new Date())+".jpg";
        File imageFile = new File(imageFilePath,timestamp);// 通过路径创建保存文件
        Uri imageFileUri = Uri.fromFile(imageFile);// 获取文件的Uri

        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
        Log.i("mylog", imageFilePath+timestamp);
        cur_cam_file_path = imageFilePath+timestamp;

        this.startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {

            Log.i("mylog", cur_cam_file_path);
            File file = new File(cur_cam_file_path);

            if(file.isFile()){
                Log.i("mylog", file.getName());
                Log.i("mylog", String.valueOf(file.length()));
                uploadFile(file);
            }
            else {
                Log.i("mylog", "failed to load file");
            }
        }
        //选择图片
        else if(requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK){
            Uri uri = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            //picturePath就是图片在储存卡所在的位置
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            File file = new File(picturePath);

            if(file.isFile()){
                Log.i("mylog", file.getName());
                Log.i("mylog", String.valueOf(file.length()));
                uploadFile(file);
            }
            else {
                Log.i("mylog", "failed to select file");
            }

        }

    }

    /** 保存相机的图片 **/
    private void saveCameraImage(Intent data) {

    }

    //上传图片到服务器
    public void uploadFile(File file) {

        OkHttpClient.Builder client_builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)   //Sets the default connect timeout for new connections.
                .writeTimeout(30, TimeUnit.SECONDS)     //Sets the default read timeout for new connections.
                .readTimeout(30, TimeUnit.SECONDS);
        OkHttpClient mOkHttpClient = client_builder.build();

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("img", "to_upload.jpg",
                        RequestBody.create(MEDIA_TYPE_JPG, file));

        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        mOkHttpClient.newCall(request)
        .enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("mylog", e.getMessage());
                Log.i("mylog", "failed to call");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                content = response.body().string();
                Log.i("mylog", content);
                res_handler.post(runnableui);
            }
        });

    }

//    public void uploadFile(File file) {
//        OkHttpClient mOkHttpClient = new OkHttpClient();
//        Request request = new Request.Builder()
//                .url(url)
//                .build();
//
//        Call call = mOkHttpClient.newCall(request);
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.i("mylog", "failed to upload");
//                Log.e("mylog", e.getMessage());
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                Log.i("mylog", "runs here");
//                Log.i("mylog", response.body().string());
//            }
//        });
//    }

    private void showResponse(){

        try{
            textView.setText("");//清空显示
            textView.setText("susucessly upload");
        }
        catch (Exception e)
        {
            Log.e("mylog",e.getMessage());
        }
    }

    private Runnable runnableui = new Runnable(){
        @Override
        public void run(){
            try{
                textView.setText("");//清空显示
                textView.setText(content);
            }
            catch (Exception e)
            {
                Log.e("mylog",e.getMessage());
            }
        }
    };

}

