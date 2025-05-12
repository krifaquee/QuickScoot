package com.example.quickscoot;

import android.os.Bundle;
import android.widget.ExpandableListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FAQActivity extends AppCompatActivity {

    ExpandableListView expandableListView;
    List<String> listGroupTitles;
    HashMap<String, List<String>> listData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        expandableListView = findViewById(R.id.expandableListView);
        prepareListData();

        FAQExpandableListAdapter adapter = new FAQExpandableListAdapter(this, listGroupTitles, listData);
        expandableListView.setAdapter(adapter);
    }

    private void prepareListData() {
        listGroupTitles = new ArrayList<>();
        listData = new HashMap<>();

        // Заголовки групп
        listGroupTitles.add("Как пользоваться приложением?");
        listGroupTitles.add("Как найти ближайший самокат?");
        listGroupTitles.add("Как начать поездку?");
        listGroupTitles.add("Сколько стоит поездка?");
        listGroupTitles.add("Можно ли у вас купить самокат?");

        // Элементы групп
        List<String> howToUse = new ArrayList<>();
        howToUse.add("Чтобы пользоваться приложением в полной мере, необходимо пройти авторизацию (Вы это уже сделали, если читаете) " +
                "и добавить карту. На вашем главном экране есть кнопки с помощью которых, вы можете начать поездку.");

        List<String> findScooter = new ArrayList<>();
        findScooter.add("Используйте карту в приложении для поиска ближайших самокатов. " +
                "Также Вы можете воспользоваться списком самокатов для поиска ближайших свободных самокатов.");

        List<String> startRide = new ArrayList<>();
        startRide.add("Отсканируйте QR-код на самокате с помощью кнопки 'Начать поездку'. Следом нажмите кнопку 'Поехали' и выберите время поездки.");

        List<String> priceRide = new ArrayList<>();
        priceRide.add("Поездка стоит 25 копеек в минуту. То есть за 20 минут - 5 рублей");

        List<String> Buy = new ArrayList<>();
        Buy.add("Нет)");

        listData.put(listGroupTitles.get(0), howToUse);
        listData.put(listGroupTitles.get(1), findScooter);
        listData.put(listGroupTitles.get(2), startRide);
        listData.put(listGroupTitles.get(3), priceRide);
        listData.put(listGroupTitles.get(4), Buy);
    }
}

