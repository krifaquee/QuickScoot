package com.example.quickscoot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity {

    private EditText cardNumber, cardExpiry, cardCVV;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        cardNumber = findViewById(R.id.card_number);
        cardExpiry = findViewById(R.id.card_expiry);
        cardCVV = findViewById(R.id.card_cvv);
        saveButton = findViewById(R.id.save_button);

        cardNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            private int previousLength;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousLength = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;

                String digitsOnly = s.toString().replaceAll("\\s", "");
                if (digitsOnly.length() > 16) {
                    digitsOnly = digitsOnly.substring(0, 16);
                }

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digitsOnly.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digitsOnly.charAt(i));
                }

                cardNumber.removeTextChangedListener(this);
                cardNumber.setText(formatted.toString());
                cardNumber.setSelection(formatted.length());
                cardNumber.addTextChangedListener(this);

                isFormatting = false;
            }
        });

        cardExpiry.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            private int previousLength;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousLength = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Нет действия здесь
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;

                String digitsOnly = s.toString().replaceAll("/", "");
                if (digitsOnly.length() > 4) {
                    digitsOnly = digitsOnly.substring(0, 4);
                }

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digitsOnly.length(); i++) {
                    if (i == 2) {
                        formatted.append("/");
                    }
                    formatted.append(digitsOnly.charAt(i));
                }

                cardExpiry.removeTextChangedListener(this);
                cardExpiry.setText(formatted.toString());
                cardExpiry.setSelection(formatted.length());
                cardExpiry.addTextChangedListener(this);

                isFormatting = false;
            }
        });

        saveButton.setOnClickListener(v -> saveCardData());

        loadCardData();
    }

    private void saveCardData() {
        String cardNumberText = cardNumber.getText().toString();
        String cardExpiryText = cardExpiry.getText().toString();
        String cardCVVText = cardCVV.getText().toString();

        if (cardNumberText.isEmpty() || cardExpiryText.isEmpty() || cardCVVText.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("userProfile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("cardNumber", cardNumberText);
        editor.putString("cardExpiry", cardExpiryText);
        editor.putString("cardCVV", cardCVVText);
        editor.apply();

        Toast.makeText(this, "Данные карты сохранены", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void loadCardData() {
        SharedPreferences sharedPreferences = getSharedPreferences("userProfile", Context.MODE_PRIVATE);
        String cardNumberText = sharedPreferences.getString("cardNumber", "");
        String cardExpiryText = sharedPreferences.getString("cardExpiry", "");
        String cardCVVText = sharedPreferences.getString("cardCVV", "");

        cardNumber.setText(cardNumberText);
        cardExpiry.setText(cardExpiryText);
        cardCVV.setText(cardCVVText);
    }
}
