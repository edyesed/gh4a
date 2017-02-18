package com.gh4a.loader;

import android.content.Context;

import com.gh4a.holder.Trend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TrendLoader extends BaseLoader<List<Trend>> {
    private final String mUrl;

    public TrendLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    public List<Trend> doLoadInBackground() throws Exception {
        URL url = new URL(mUrl);
        List<Trend> trends = new ArrayList<>();

        HttpURLConnection connection = null;
        CharArrayWriter writer = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return trends;
            }

            InputStream in = new BufferedInputStream(connection.getInputStream());
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");
            int length = connection.getContentLength();
            writer = new CharArrayWriter(Math.max(0, length));
            char[] tmp = new char[4096];

            int l;
            while ((l = reader.read(tmp)) != -1) {
                writer.write(tmp, 0, l);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (writer != null) {
                writer.close();
            }
        }

        JSONArray resultArray = new JSONArray(writer.toString());
        for (int i = 0; i < resultArray.length(); i++) {
            JSONObject repoObject = resultArray.getJSONObject(i);

            trends.add(new Trend(
                    repoObject.getString("owner"),
                    repoObject.getString("repo"),
                    repoObject.optString("description"),
                    (int) repoObject.getDouble("stars"),
                    (int) repoObject.getDouble("new_stars"),
                    (int) repoObject.getDouble("forks")));
        }
        return trends;
    }
}
