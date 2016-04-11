package space.klapeyron.robotmgok;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.List;

public class QRFragment extends Fragment {
    private CompoundBarcodeView barcodeView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.qr_fragment, null);
        barcodeView = (CompoundBarcodeView) v.findViewById(R.id.barcode_scanner);
        barcodeView.decodeContinuous(barcodeCallback);

        CameraSettings settings = new CameraSettings();
        settings.setRequestedCameraId(1);
        barcodeView.getBarcodeView().setCameraSettings(settings);

        barcodeView.setStatusText("");
        return v;
    }

    @Override
    public void onResume() {
        barcodeView.resume();
        super.onResume();
    }

    @Override
    public void onPause() {
        barcodeView.pause();
        super.onPause();
    }

    private BarcodeCallback barcodeCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            Log.d("TAG", "Result");
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };
}
