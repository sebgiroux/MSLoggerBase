package uk.org.smithfamily.mslogger.dashboards;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.org.smithfamily.mslogger.ApplicationSettings;
import uk.org.smithfamily.mslogger.log.DebugLogManager;
import uk.org.smithfamily.mslogger.widgets.Indicator;
import uk.org.smithfamily.mslogger.widgets.Indicator.DisplayType;
import uk.org.smithfamily.mslogger.widgets.Location;
import android.content.res.AssetManager;

public class DashboardIO
{
    private static final String DASHBOARDS = "dashboards";
    private static final String INDICATORS = "indicators";
    private static final String LOCATION = "location";
    private static final String TYPE = "type";
    private static final String CHANNEL = "channel";

    public List<Dashboard> parse() throws IOException
    {
        List<Dashboard> results = new ArrayList<Dashboard>();
        try
        {
            String dashDefinition = readDefinition("default.json");

            JSONObject jsonObject = new JSONObject(dashDefinition);

            JSONArray dashes = jsonObject.getJSONArray(DASHBOARDS);

            for (int dashIndex = 0; dashIndex < dashes.length(); dashIndex++)
            {
                JSONObject jDash = dashes.getJSONObject(dashIndex);
                results.add(createDash(jDash));
            }
        }
        catch (JSONException e)
        {
            DebugLogManager.INSTANCE.logException(e);
        }
        return results;
    }

    private Dashboard createDash(JSONObject jDash) throws JSONException
    {
        Dashboard d = new Dashboard();
        JSONArray indicators = jDash.getJSONArray(INDICATORS);
        for (int indIndex = 0; indIndex < indicators.length(); indIndex++)
        {
            JSONObject jIndicator = indicators.getJSONObject(indIndex);
            Indicator i = createIndicator(jIndicator);

            d.add(i);
        }

        return d;
    }

    private Indicator createIndicator(JSONObject jIndicator) throws JSONException
    {
        Indicator i = new Indicator(ApplicationSettings.INSTANCE.getContext());
        String channel = jIndicator.getString(CHANNEL);
        String type = jIndicator.getString(TYPE).toUpperCase();
        JSONArray jLocation = jIndicator.getJSONArray(LOCATION);
        Location loc = createLocation(jLocation);
        i.setChannel(channel);
        i.setDisplayType(DisplayType.valueOf(type));
        i.setLocation(loc);

        return i;
    }

    private Location createLocation(JSONArray jLocation) throws JSONException
    {
        return new Location(jLocation.getDouble(0), jLocation.getDouble(1), jLocation.getDouble(2), jLocation.getDouble(3));
    }

    private String readDefinition(String fileName)
    {
        StringBuilder sb = new StringBuilder();
        String assetFileName = DASHBOARDS + File.separator + fileName;
        File override = new File(ApplicationSettings.INSTANCE.getDataDir(), fileName);

        AssetManager assetManager = ApplicationSettings.INSTANCE.getContext().getResources().getAssets();

        BufferedReader input = null;
        try
        {
            try
            {
                InputStream data = null;
                if (override.canRead())
                {
                    data = new FileInputStream(override);
                }
                else
                {
                    data = assetManager.open(assetFileName);
                }

                input = new BufferedReader(new InputStreamReader(data));
                String line;

                while ((line = input.readLine()) != null)
                {
                    sb.append(line);
                }
                data.close();
            }
            finally
            {
                if (input != null)
                    input.close();
            }

        }
        catch (IOException e)
        {
            DebugLogManager.INSTANCE.logException(e);
        }
        return sb.toString();
    }
}