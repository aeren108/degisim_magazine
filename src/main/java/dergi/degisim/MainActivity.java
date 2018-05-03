// -*- @author aeren_pozitif  -*- //
package dergi.degisim;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import dergi.degisim.auth.LoginActivity;
import dergi.degisim.drawer.ItemAdapter;
import dergi.degisim.fragment.HomeFragment;
import dergi.degisim.fragment.MarkedFragment;
import dergi.degisim.fragment.WeeklyFragment;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener, FirebaseAuth.AuthStateListener {

    public DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;

    public ListView applicationList;
    public ListView categoryList;

    private ItemAdapter applicationAdapter;
    private ItemAdapter categoryAdapter;

    public String[] applicationTitles;
    public String[] categoryTitles;

    public static List<String> marks = null;

    private FirebaseAuth auth = FirebaseAuth.getInstance();

    public BottomNavigationView navigation;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    HomeFragment homeFragment = new HomeFragment();
                    getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, homeFragment).commit();
                    return true;
                case R.id.navigation_dashboard:
                    MarkedFragment postsFragment = new MarkedFragment();
                    getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, postsFragment).commit();
                    return true;
                case R.id.navigation_notifications:
                    WeeklyFragment weeklyFragment = new WeeklyFragment();
                    getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, weeklyFragment).commit();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        drawer = findViewById(R.id.drawer_layout);
        categoryList = findViewById(R.id.category_list);
        applicationList = findViewById(R.id.application_list);

        categoryTitles = getResources().getStringArray(R.array.category_items);
        applicationTitles = getResources().getStringArray(R.array.application_items);

        categoryAdapter = new ItemAdapter(Arrays.asList(categoryTitles), getApplicationContext());
        categoryList.setAdapter(categoryAdapter);

        applicationAdapter = new ItemAdapter(Arrays.asList(applicationTitles), getApplicationContext());
        applicationList.setAdapter(applicationAdapter);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            applicationAdapter.editItem(0, "Çıkış Yap");
            applicationList.setAdapter(applicationAdapter);
            applicationList.invalidate();
        }

        applicationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        Intent intent = new Intent(getApplication(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                    } else {
                        FirebaseAuth.getInstance().signOut();
                    }
                    drawer.closeDrawers();
                }
            }
        });

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toggle = new ActionBarDrawerToggle(this, drawer, R.string.opened, R.string.closed);

        drawer.setDrawerListener(toggle);
        toggle.syncState();

        auth.addAuthStateListener(this);

        HomeFragment home = new HomeFragment();
        getSupportFragmentManager().beginTransaction().
        replace(R.id.frame, home).commit();
        for (int i = 0; i < HomeFragment.NEWS_AMOUNT; i++) {
            if (home.u != null)
                home.u.fetchData("id", i);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchItem.setOnActionExpandListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        HomeFragment home = new HomeFragment();
        getSupportFragmentManager().beginTransaction().
        replace(R.id.frame, home).commit();

        home.performSearchQuery(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        HomeFragment home = new HomeFragment();
        getSupportFragmentManager().beginTransaction().
        replace(R.id.frame, home).commit();
        navigation.setSelectedItemId(R.id.navigation_home);

        return true;
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser usr = firebaseAuth.getCurrentUser();
        if (usr != null) {
            if (usr.isAnonymous()) {
                applicationAdapter.editItem(0, "Giriş Yap");
                applicationList.setAdapter(applicationAdapter);
                applicationList.invalidate();
            } else {
                applicationAdapter.editItem(0, "Çıkış Yap");
                applicationList.setAdapter(applicationAdapter);
                applicationList.invalidate();
            }
        } else {
            applicationAdapter.editItem(0, "Giriş Yap");
            applicationList.setAdapter(applicationAdapter);
            applicationList.invalidate();

            auth.signInAnonymously().addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    Log.d("AUTH", "Logged in anonymously");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("AUTH", "Couldn't logged in anonymously");
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        Fragment curFragment = getSupportFragmentManager().findFragmentById(R.id.frame);
        if (curFragment instanceof HomeFragment) {
            if (((HomeFragment) curFragment).mode == 'c') {
                HomeFragment home = new HomeFragment();
                getSupportFragmentManager().beginTransaction().
                replace(R.id.frame, home).commit();
            }
        } if (curFragment instanceof WeeklyFragment) {
            if (((WeeklyFragment) curFragment).catMode) {
                WeeklyFragment weeklyFragment = new WeeklyFragment();
                getSupportFragmentManager().beginTransaction().
                replace(R.id.frame, weeklyFragment).commit();
            }
        } else
            super.onBackPressed();
    }
}