// -*- @author aeren_pozitif  -*- //
package dergi.degisim.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.r0adkll.slidr.Slidr;
import com.shobhitpuri.custombuttons.GoogleSignInButton;

import dergi.degisim.MainActivity;
import dergi.degisim.R;
import dergi.degisim.db.Database;

public class LoginActivity extends AppCompatActivity {
    private EditText email;
    private EditText pswd;
    private Button login;
    private Button register;
    private GoogleSignInButton glogin;

    private GoogleApiClient apiClient;
    private FirebaseAuth auth = FirebaseAuth.getInstance();

    private final static int RC_SIGN_IN = 1302;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Slidr.attach(this);

        email = findViewById(R.id.email_input);
        pswd = findViewById(R.id.password_input);
        login = findViewById(R.id.login_button);
        register = findViewById(R.id.register_btn);
        glogin = findViewById(R.id.signInButton);

        login.setOnClickListener(v -> login());
        register.setOnClickListener(v -> register());
        glogin.setOnClickListener(v -> signIn());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).
        requestIdToken("1040413724850-0qvare5jk9qpv0vupqjmumkm5evclnrf.apps.googleusercontent.com").requestEmail().build();

        //apiClient = new GoogleApiClient.Builder(getApplicationContext()).enableAutoManage(this, connectionResult -> Toast.makeText(getApplicationContext(), "Bağlantı başarısız oldu", Toast.LENGTH_LONG).show()).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();
    }

    private boolean checkCreditentials(String email, String pswd) {
        if (email.isEmpty() || pswd.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Alanları doldurun", Toast.LENGTH_SHORT).show();
            return true;
        } else if (!email.contains("@")) {
            Toast.makeText(getApplicationContext(), "Geçerli bir e-posta girin", Toast.LENGTH_SHORT).show();
            return true;
        } else if (email.split("@")[1].split(".").length != 2){
            Toast.makeText(getApplicationContext(), "Geçerli bir e-posta girin", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void login() {
        String em = email.getText().toString();
        String pd = pswd.getText().toString();

        if (checkCreditentials(em, pd))
            return;

        if (Database.checkLoggedIn())
            auth.signOut();

        auth.signInWithEmailAndPassword(em, pd).addOnSuccessListener(authResult -> {
            Toast.makeText(getApplicationContext(), "Giriş yapıldı", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);

            finish();
        }).addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Parola veya e-posta doğru değil", Toast.LENGTH_LONG).show());
    }

    private void register() {
        String em = email.getText().toString();
        String pd = pswd.getText().toString();

        if (checkCreditentials(em, pd))
            return;

        if (Database.checkLoggedIn())
            auth.signOut();

        auth.createUserWithEmailAndPassword(em, pd).addOnSuccessListener(authResult -> {
            Toast.makeText(getApplicationContext(), "Başarılı", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);

            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference ref = db.getReference("users");
            ref.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("markeds").setValue("empty");

            finish();
        }).addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Bir şeyler yanlış oldu, tüh :(", Toast.LENGTH_LONG).show());
    }

    private void signIn() {
        if (auth.getCurrentUser() != null)
            if (auth.getCurrentUser().isAnonymous())
                auth.signOut();

        Intent intent = Auth.GoogleSignInApi.getSignInIntent(apiClient);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w("AUTH", "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Toast.makeText(getApplicationContext(), "Authenticating ", Toast.LENGTH_SHORT).show();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        auth.signInWithCredential(credential).
        addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);

                FirebaseDatabase db = FirebaseDatabase.getInstance();
                DatabaseReference ref = db.getReference("users");

                if (task.getResult().getAdditionalUserInfo().isNewUser())
                    ref.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("markeds").setValue("empty");

                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Giriş yapılamadı", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
