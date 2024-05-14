package com.example.newhwk2;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_STORAGE = 100;
    private static final String ENCRYPTION_KEY = "eecs700";

    private Button encryptButton, decryptButton;
    private EditText keyEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        encryptButton = findViewById(R.id.encrypt_button);
        decryptButton = findViewById(R.id.decrypt_button);
        keyEditText = findViewById(R.id.key_edit_text);

        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermissions();
                encryptFiles();
            }
        });

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermissions();
                decryptFiles();
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // User has previously denied the permission and selected "Don't ask again"
                // Redirect the user to the app settings to grant the permission manually
                openAppSettings();
                return;
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted. You can proceed.", Toast.LENGTH_SHORT).show();
            } else {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // User has denied the permission and selected "Don't ask again"
                    openAppSettings();
                } else {
                    Toast.makeText(this, "Storage permission denied. Cannot proceed.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, PERMISSION_REQUEST_STORAGE);
    }

    private void encryptFiles() {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (isDocumentFile(file) && !file.getName().endsWith(".encrypted")) {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        FileOutputStream fos = new FileOutputStream(file.getPath() + ".encrypted");
                        encryptStream(fis, fos, ENCRYPTION_KEY);
                        fis.close();
                        fos.close();
                        // Delete the original unencrypted file after encryption
                        file.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Toast.makeText(this, "Files encrypted successfully.", Toast.LENGTH_SHORT).show();
        }
    }

    private void decryptFiles() {
        String key = keyEditText.getText().toString();
        if (key.isEmpty()) {
            Toast.makeText(this, "Please enter decryption password.", Toast.LENGTH_SHORT).show();
            return;
        }

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".encrypted")) {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        FileOutputStream fos = new FileOutputStream(file.getPath().replace(".encrypted", ""));
                        decryptStream(fis, fos, key);
                        fis.close();
                        fos.close();
                        // No need to delete the encrypted file after decryption
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Toast.makeText(this, "Files decrypted successfully.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isDocumentFile(File file) {
        String fileName = file.getName();
        return fileName.endsWith(".pdf") || fileName.endsWith(".doc") || fileName.endsWith(".docx")
                || fileName.endsWith(".ppt") || fileName.endsWith(".pptx");
    }

    private void encryptStream(InputStream is, OutputStream os, String key) throws IOException {
        try {
            byte[] keyBytes = key.getBytes();
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            CipherOutputStream cos = new CipherOutputStream(os, cipher);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
            cos.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure streams are properly closed
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    private void decryptStream(InputStream is, OutputStream os, String key) throws IOException {
        try {
            byte[] keyBytes = key.getBytes();
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            CipherInputStream cis = new CipherInputStream(is, cipher);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            cis.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure streams are properly closed
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }
}
