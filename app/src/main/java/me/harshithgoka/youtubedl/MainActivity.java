package me.harshithgoka.youtubedl;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.harshithgoka.youtubedl.Utils.Utils;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 9293;

    EditText urlEdit;
    TextView log;
    FloatingActionButton btnCopy;
    Button btnDownload, btnAllFormats;

    List<Format> formats;

    Extractor extractor;

    Pattern youtubeUrlPattern;

    RecyclerView recyclerView;
    FormatAdapter adapter;
    LinearLayoutManager linearLayoutManager;

    BottomSheetBehavior<View> bottomSheetBehavior;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

        }

        this.btnAllFormats = findViewById(R.id.btnAllFormats);
        this.btnCopy = findViewById(R.id.fab);
        this.btnDownload = findViewById(R.id.btnDownload);

        this.btnAllFormats.setOnClickListener(this);
        this.btnCopy.setOnClickListener(this);
        this.btnDownload.setOnClickListener(this);

        log = (TextView) findViewById(R.id.textView);
        log.setMovementMethod(new ScrollingMovementMethod());

        urlEdit = (EditText) findViewById(R.id.url);

        formats = new ArrayList<>();
        extractor = new Extractor();

        youtubeUrlPattern = Pattern.compile(extractor._VALID_URL);

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        recyclerView = findViewById(R.id.recycler_view);
        adapter = new FormatAdapter(getApplicationContext(), formats);
        recyclerView.setAdapter(adapter);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        BottomAppBar bar = (BottomAppBar) findViewById(R.id.bar);
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the navigation click by showing a BottomDrawer etc.
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        ClipData clipData = appLinkIntent.getClipData();
        Uri appLinkData = appLinkIntent.getData();
        String url = null;
        if (appLinkData != null) {
            url = appLinkData.toString();
        }
        else if (clipData != null) {
            if (clipData.getItemCount() > 0)
                url = clipData.getItemAt(0).getText().toString();
        }

        if (url != null) {
            urlEdit.setText(url);
            startDownload(url);
        }
    }

    private void println (String s) {
        log.append(s + "\n");
    }

    public String preprocess (String s) {
        int index = s.lastIndexOf("#");
        if (index > 0) {
            s = s.substring(0, index);
        }

        s = s.replaceFirst("m.youtube.com", "www.youtube.com");
        s = s.replaceFirst("&.*", "");

        return s;
    }

    public void startDownload(String url) {
        url = preprocess(url);

        java.util.regex.Matcher m = youtubeUrlPattern.matcher(url);
        println("Url: " + url);

        urlEdit.setText(url);

        if (!m.find()) {
            Toast.makeText(this, "Invalid Youtube URL", Toast.LENGTH_SHORT).show();
            return;
        }

        AsyncTask<String, Void, List<Format>> asyncTask = new YoutubeDLAsyncTask(getApplicationContext(), extractor);
        asyncTask.execute(url);
    }

    public void startPoint(View button) {
        String url = urlEdit.getText().toString();
        startDownload(url);
    }

    public void pasteFromClipboard(View button) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clipData = clipboard.getPrimaryClip();
            Uri uri;
            CharSequence url;
            if ( clipData.getItemCount() > 0 ) {
                if ((uri = clipData.getItemAt(0).getUri()) != null) {
                    urlEdit.setText(uri.toString());
                }
                else if ((url = clipData.getItemAt(0).getText()) != null ) {
                    urlEdit.setText(url.toString());
                }
                startPoint(button);
            }
        }
    }

    public void showAllFormats(View view) {
        if (formats.size() > 0) {
            Intent intent = new Intent(getApplicationContext(), FormatsActivity.class);
            intent.putParcelableArrayListExtra(FormatsActivity.FORMATS, (ArrayList<? extends Parcelable>) formats);
            startActivity(intent);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnAllFormats:
                showAllFormats(v);
                break;

            case R.id.fab:
                pasteFromClipboard(v);
                break;

            case R.id.btnDownload:
                startPoint(v);
                break;
        }
    }

    class YoutubeDLAsyncTask extends AsyncTask<String, Void, List<Format>> {
        Context context;
        Extractor ytextractor;

        public YoutubeDLAsyncTask(Context context, Extractor extractor) {
            this.context = context;
            ytextractor = extractor;
        }


        @Override
        protected List<Format> doInBackground(String... strings) {
            String you_url = strings[0];
            return ytextractor.getFormats(you_url);
        }

        @Override
        protected void onPostExecute(List<Format> formats) {
            if (formats != null) {
                if (formats.size() > 0) {
                    MainActivity.this.formats.clear();
                    MainActivity.this.formats.addAll(formats);
                    MainActivity.this.adapter.notifyDataSetChanged();

                    String finalurl = formats.get(0).url;
                    println(finalurl);

                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    assert clipboard != null;
                    ClipData clip = ClipData.newRawUri("DownloadURL", Uri.parse(finalurl));
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(getApplicationContext(), String.format("Best quality link (%s) copied to Clipboard", formats.get(0).quality), Toast.LENGTH_SHORT).show();
                }
                else {
                    println("No. of formats: 0");
                    Toast.makeText(getApplicationContext(), "Not yet implemented encrypted signature", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                println("Error connecting to the Internet");
            }
        }
    };
}
