package flag.com.tw.ch10_camera4;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    Uri imgUri; // 拍照存檔的uri
    ImageView imv; // imageView的物件

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        imv = (ImageView) findViewById(R.id.imageView);
    }

    public void onGet(View v){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != // 檢查是否取得權限
                PackageManager.PERMISSION_GRANTED){
            // 尚未取得權限
            ActivityCompat.requestPermissions(this, // 向使用者要求權限
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        }else savePhoto();
    }

    private void savePhoto() {
        imgUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,  // 透過內容資料庫新增一個圖片檔
                new ContentValues());

        Intent it = new Intent("android.media.action.IMAGE_CAPTURE");
        it.putExtra(MediaStore.EXTRA_OUTPUT, imgUri); // 將uri加到拍照intent的額外資料中

        startActivityForResult(it, 100); // 啟動intent並要求回傳資料
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == 200){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){ // 使用者允許權限
                savePhoto();
            }
        }else Toast.makeText(this, "請寫入權限", Toast.LENGTH_SHORT).show(); // 使用者拒絕權限
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == Activity.RESULT_OK){
            switch (requestCode) {
                case 100:
                    Intent it = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imgUri);

                    sendBroadcast(it);
                    break;
                case 101:
                    imgUri = data.getData();
                    break;
            }
            showImg();

        }else {
            Toast.makeText(this, requestCode == 100 ? "沒有拍到照片" : "沒有選取照片", Toast.LENGTH_LONG).show();
        }
    }

    public void onPick(View v){
        Intent it = new Intent(Intent.ACTION_GET_CONTENT);
        it.setType("image/*");
        startActivityForResult(it, 100);
    }

    void showImg() {
        int iw, ih, vw, vh;
        Boolean needRotate;


        BitmapFactory.Options option = new BitmapFactory.Options(); // 建立選項物件

        option.inJustDecodeBounds = true;

        try{
            BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(imgUri), null, option
            );
        }catch (IOException e){
            Toast.makeText(this, "讀取照片資訊錯誤", Toast.LENGTH_LONG).show();

            return;
        }

        iw = option.outWidth;
        ih = option.outHeight;

        vw = imv.getWidth();
        vh = imv.getHeight();

        int scaleFactor;

        if(iw < ih){
            needRotate = false;
            scaleFactor = Math.min(iw / vw, ih / vh);
        }else{
            needRotate = true;
            scaleFactor = Math.min(ih / vw, iw / vh);
        }



        option.inJustDecodeBounds = false;
        option.inSampleSize = scaleFactor;

        Bitmap bmp = null;

        try{
            bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(imgUri), null, option);
        }catch (IOException e){
            Toast.makeText(this, "無法取得照片", Toast.LENGTH_LONG).show();
        }

        if(needRotate){
            Matrix matrix = new Matrix();
            matrix.postRotate(90);

            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        }

        imv.setImageBitmap(bmp);

        new AlertDialog.Builder(this)
                .setTitle("圖檔資訊")
                .setMessage("圖檔URI：" + imgUri.toString() +
                        "\n原始尺寸：" + iw + " * " + vw +
                        "\n載入尺寸：" + bmp.getWidth() + " * " + bmp.getHeight() +
                        "\n顯示尺寸：" + vw + " * " + vh +
                        (needRotate ? "(旋轉)" : ""))
                .setNeutralButton("關閉", null)
                .show();
    }
}
