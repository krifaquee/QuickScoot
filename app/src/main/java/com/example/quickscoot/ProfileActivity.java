package com.example.quickscoot;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView profileImage;
    private EditText firstName, lastName, phoneNumber;
    private TextView birthDate;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImage = findViewById(R.id.profile_image);
        firstName = findViewById(R.id.first_name);
        lastName = findViewById(R.id.last_name);
        phoneNumber = findViewById(R.id.phone_number);
        birthDate = findViewById(R.id.birth_date);
        saveButton = findViewById(R.id.save_button);

        profileImage.setOnClickListener(v -> openGallery());

        birthDate.setOnClickListener(v -> openDatePickerDialog());

        saveButton.setOnClickListener(v -> {
            saveProfileData();
            Toast.makeText(this, "Данные профиля сохранены", Toast.LENGTH_SHORT).show();
            finish();  // Закрытие активности после сохранения данных
        });


        loadProfileData();

        firstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                saveProfileData();
            }
        });

        lastName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                saveProfileData();
            }
        });

        phoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                saveProfileData();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                profileImage.setImageBitmap(bitmap);
                saveImage(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            birthDate.setText(dayOfMonth + "/" + (month1 + 1) + "/" + year1);
            saveProfileData();
        }, year, month, day);

        datePickerDialog.show();
    }

    private void saveProfileData() {
        String firstNameText = firstName.getText().toString();
        String lastNameText = lastName.getText().toString();
        String phoneNumberText = phoneNumber.getText().toString();
        String birthDateText = birthDate.getText().toString();

        SharedPreferences sharedPreferences = getSharedPreferences("userProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("firstName", firstNameText);
        editor.putString("lastName", lastNameText);
        editor.putString("phoneNumber", phoneNumberText);
        editor.putString("birthDate", birthDateText);
        editor.apply();

        Intent intent = new Intent("PROFILE_UPDATED");
        sendBroadcast(intent);
    }


    private void saveImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        SharedPreferences sharedPreferences = getSharedPreferences("userProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("profileImage", encodedImage);
        editor.apply();
    }

    private void loadProfileData() {
        SharedPreferences sharedPreferences = getSharedPreferences("userProfile", Context.MODE_PRIVATE);
        String firstNameText = sharedPreferences.getString("firstName", "");
        String lastNameText = sharedPreferences.getString("lastName", "");
        String phoneNumberText = sharedPreferences.getString("phoneNumber", "");
        String birthDateText = sharedPreferences.getString("birthDate", "");
        String encodedImage = sharedPreferences.getString("profileImage", "");

        firstName.setText(firstNameText);
        lastName.setText(lastNameText);
        phoneNumber.setText(phoneNumberText);
        birthDate.setText(birthDateText);

        if (!encodedImage.isEmpty()) {
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            profileImage.setImageBitmap(decodedByte);
        }
    }
}
