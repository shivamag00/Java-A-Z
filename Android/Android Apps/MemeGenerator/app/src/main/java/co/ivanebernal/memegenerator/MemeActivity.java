package co.ivanebernal.memegenerator;

import android.Manifest.permission;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import butterknife.ButterKnife;
import co.ivanebernal.memegenerator.MemeViewModel.Factory;
import co.ivanebernal.memegenerator.TextPropertiesView.OnPropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MemeActivity extends AppCompatActivity {

    private MemeViewModel mViewModel;

    @BindView(R.id.uploadImageButton)
    Button uploadImageButton;
    @BindView(R.id.memeImage)
    ImageView memeImage;

    @BindView(R.id.topTextProperties)
    TextPropertiesView topTextProperties;
    @BindView(R.id.bottomTextProperties)
    TextPropertiesView bottomTextProperties;

    @BindView(R.id.saveMemeButton)
    Button saveMemeButton;
    @BindView(R.id.changeImageButton)
    Button changeImageButton;

    @BindView(R.id.editContainer)
    LinearLayout editContainer;

    private static final int PICK_IMAGE = 1;
    private static final int WRITE_EXTERNAL_STORAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        initViewModel();
        observeValues();

        setupViews();
    }

    private void setupViews() {
        topTextProperties.setOnPropertyChangeListener(new OnPropertyChangeListener() {
            @Override
            public void onTextChanged(final String text) {
                mViewModel.setTopText(text);
            }

            @Override
            public void onColorChanged(final Integer color) {
                mViewModel.setTopTextColor(color);
            }

            @Override
            public void onSizeChanged(final Integer size) {
                mViewModel.setTopTextSize(size);
            }
        });

        bottomTextProperties.setOnPropertyChangeListener(new OnPropertyChangeListener() {
            @Override
            public void onTextChanged(final String text) {
                mViewModel.setBottomText(text);
            }

            @Override
            public void onColorChanged(final Integer color) {
                mViewModel.setBottomTextColor(color);
            }

            @Override
            public void onSizeChanged(final Integer size) {
                mViewModel.setBottomTextSize(size);
            }
        });

        uploadImageButton.setOnClickListener(view -> {
            openImageSelection();
        });

        saveMemeButton.setOnClickListener(view -> {
            askGalleryPermission();
        });

        changeImageButton.setOnClickListener(view -> {
            openImageSelection();
        });
    }

    private void initViewModel() {
        Factory memeVMFactory = new Factory(this);
        ViewModelProvider vmProvider = new ViewModelProvider(this, memeVMFactory);
        mViewModel = vmProvider.get(MemeViewModel.class);
    }

    private void observeValues() {
        mViewModel.getTopTextObservable().observe(this, this::setTopText);
        mViewModel.getTopTextColorObservable().observe(this, this::setTopTextColor);
        mViewModel.getTopTextSizeObservable().observe(this, this::setTopTextSize);

        mViewModel.getBottomTextObservable().observe(this, this::setBottomText);
        mViewModel.getBottomTextColorObservable().observe(this, this::setBottomTextColor);
        mViewModel.getBottomTextSizeObservable().observe(this, this::setBottomTextSize);

        mViewModel.getGeneratedMemeObservable().observe(this, this::setImage);
    }

    //Recover values on config change
    private void setImage(Bitmap image) {
        if (image == null) {
            editContainer.setVisibility(View.GONE);
            uploadImageButton.setVisibility(View.VISIBLE);
        } else {
            editContainer.setVisibility(View.VISIBLE);
            uploadImageButton.setVisibility(View.GONE);
            memeImage.setImageBitmap(image);
        }
    }

    private void setTopTextColor(Integer color) {
        topTextProperties.setTextColor(color);
    }

    private void setTopText(String text) {
        topTextProperties.setText(text);
    }

    private void setTopTextSize(Integer size) {
        topTextProperties.setTextSize(size);
    }

    private void setBottomTextColor(Integer color) {
        bottomTextProperties.setTextColor(color);
    }

    private void setBottomText(String text) {
        bottomTextProperties.setText(text);
    }

    private void setBottomTextSize(Integer size) {
        bottomTextProperties.setTextSize(size);
    }

    private void askGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            mViewModel.onSaveMemeClicked();
        } else if (shouldShowRequestPermissionRationale(permission.WRITE_EXTERNAL_STORAGE)) {
            showExternalStoragePermissionRationale();
        } else {
            requestPermissions(new String[] {permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
        }
    }

    private void showExternalStoragePermissionRationale() {
        new AlertDialog.Builder(this)
                .setMessage("Grant Meme Generator permission to save your meme in your Gallery")
                .setPositiveButton("Ok", (dialog, which) -> {
                    //Do nothing
                }).show();
    }

    private void openImageSelection() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE) {
            if (data == null) return;
            try {
                Uri dataUri = data.getData();
                if (dataUri == null) return;
                InputStream is = getContentResolver().openInputStream(dataUri);
                Bitmap image = BitmapFactory.decodeStream(is);
                mViewModel.setImage(image);
            } catch (FileNotFoundException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
            @NonNull final int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mViewModel.onSaveMemeClicked();
                } else {
                    showExternalStoragePermissionRationale();
                }
                return;
        }

    }

}