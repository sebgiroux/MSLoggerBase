package uk.org.smithfamily.mslogger.activity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import uk.org.smithfamily.mslogger.ApplicationSettings;
import uk.org.smithfamily.mslogger.R;
import uk.org.smithfamily.mslogger.log.DebugLogManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 *
 */
public class ViewDatalogActivity extends Activity
{
    private GraphicalView mChartView;

    private String[] headers;
    private String[] completeHeaders;
    private List<List<Double>> data;
    
    private Button selectDatalogFields;
    
    public static final int BACK_FROM_DATALOG_FIELDS = 1;
    
    /**
     * 
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewdatalog);
         
        selectDatalogFields = (Button) findViewById(R.id.select_datalog_fields);
        selectDatalogFields.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent launchDatalogFields = new Intent(ViewDatalogActivity.this, DatalogFieldsActivity.class);
                
                Bundle b = new Bundle();
                b.putStringArray("datalog_fields",completeHeaders);
                launchDatalogFields.putExtras(b);
                
                startActivityForResult(launchDatalogFields,BACK_FROM_DATALOG_FIELDS);
            }
        });
        
        new readLogFileInBackground().execute((Void) null);
    }
    
    /*
     * Method called when the datalog fields have changed to refresh the graph
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == BACK_FROM_DATALOG_FIELDS)
        {
            
            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            layout.removeAllViews();
            
            mChartView = null;
            
            new readLogFileInBackground().execute((Void) null);
        }
    }
    
    /**
     * Read the datalog and fill up all the necessary variables
     */
    private void readDatalog(String datalog)
    {        
        String fieldsToKeep[] = ApplicationSettings.INSTANCE.getDatalogFields();
        int indexOfFieldsToKeep[] = new int[fieldsToKeep.length];
        
        try
        {            
            InputStream instream = new FileInputStream(datalog);

            if (instream != null)
            {
                // Prepare the file for reading
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);

                int nbLine = 0;
                headers = new String[] {};
                data = new ArrayList<List<Double>>();
                
                // Initialise list
                for (int i = 0; i < fieldsToKeep.length; i++)
                {
                    data.add(new ArrayList<Double>());
                }
                
                String line;
                String[] lineSplit;
                
                // Read every line of the file into the line-variable, on line at the time
                while ((line = buffreader.readLine()) != null) {
                    if (nbLine > 0)
                    {                    
                        lineSplit = line.split("\t");    
                        
                        if (nbLine == 1) 
                        {
                            headers = lineSplit;
                            int k = 0;
                            
                            for (int i = 0; i < headers.length; i++)
                            {
                                for (int j = 0; j < fieldsToKeep.length; j++)
                                {
                                    if (headers[i].equals(fieldsToKeep[j])) 
                                    {
                                        indexOfFieldsToKeep[k++] = i;
                                    }
                                }
                            }
                            
                            completeHeaders = headers;
                            headers = fieldsToKeep;
                        }
                        else
                        {     
                            // Skip MARK and empty line
                            if ((lineSplit[0].length() > 3 && lineSplit[0].substring(0,4).equals("MARK")) || lineSplit[0].equals(""))
                            {
                                
                            }
                            else
                            {
                                double[] dataFieldsToKeep = new double[fieldsToKeep.length];
                                for (int i = 0; i < indexOfFieldsToKeep.length; i++)
                                {
                                    double currentValue = 0;
                                    if (lineSplit.length > indexOfFieldsToKeep[i])
                                    {                                    
                                        currentValue = Double.parseDouble(lineSplit[indexOfFieldsToKeep[i]]);
                                    }
                                    
                                    dataFieldsToKeep[i] = currentValue;
                                    data.get(i).add(dataFieldsToKeep[i]);
                                }
                            }
                        }
                    }
                    
                    nbLine++;
                }
            }
        }
        catch (FileNotFoundException e)
        {
            DebugLogManager.INSTANCE.logException(e);
        } 
        catch (IOException e)
        {
            DebugLogManager.INSTANCE.logException(e);
        }      
    }
    
    /**
     * Generate graph for the selected datalog
     */
    private void generateGraph()
    {        
        double minXaxis = 0;
        double maxXaxis = data.get(0).size();
        
        // Assuming first column of datalog is time for X axis
        double[] xValues = new double[(int)maxXaxis];        
        for (int i = 0; i < maxXaxis; i++)
        {          
            xValues[i] = i;
        } 
        
        // Rebuild the headers array for title, we use them all but the first one (Time)
        String[] titles = new String[headers.length - 1];        
        for (int i = 1; i < headers.length; i++)
        {
            titles[i-1] = headers[i];
        }

        List<double[]> x = new ArrayList<double[]>();
        List<double[]> values = new ArrayList<double[]>();
             
        // Add X values for all titles
        for (int i = 0; i < titles.length; i++)
        { 
            x.add(xValues);
        }
        
        List<Double> minColumns = new ArrayList<Double>();
        List<Double> maxColumns = new ArrayList<Double>();
        
        // Find min and max value for each columns
        for (int i = 0; i < data.size(); i++) 
        {
            List<Double> row = data.get(i);
            
            double min = row.get(0); 
            double max = min;
            for (int j = 0; j < row.size(); j++)
            {
                double value = row.get(j);
                
                if (min > value)
                {
                    min = value;
                } 
                
                if (max < value)
                {
                    max = value;
                }
            }
            
            minColumns.add(min);
            maxColumns.add(max);
        }
        
        for (int i = 1; i < data.size(); i++)
        {
            List<Double> row = data.get(i);
            double[] rowDouble = new double[row.size()];
            for (int j = 0; j < row.size(); j++)
            {
                rowDouble[j] = row.get(j);
                
                // Find percent between min and max
                rowDouble[j] = (rowDouble[j] - minColumns.get(i)) / (maxColumns.get(i) - minColumns.get(i)) * 100;
            }           
            
            values.add(rowDouble);
        }
        
        XYMultipleSeriesRenderer renderer = buildRenderer(titles.length);
        setChartSettings(renderer, "", "", "", minXaxis, Math.min(100,maxXaxis), 0, 100, Color.GRAY, Color.LTGRAY);
        renderer.setYLabels(10);
        renderer.setPanLimits(new double[] { minXaxis,maxXaxis,0,100 });
        renderer.setShowLabels(false);
        
        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);

        if (mChartView == null)
        {
            mChartView = ChartFactory.getLineChartView(ViewDatalogActivity.this, buildDateDataset(titles, x, values), renderer);
            layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        }
        else
        {
            mChartView.repaint();
        }
    }

    /**
     * Builds an XY multiple time dataset using the provided values.
     * 
     * @param titles
     *            the series titles
     * @param xValues
     *            the values for the X axis
     * @param yValues
     *            the values for the Y axis
     * @return the XY multiple time dataset
     */
    protected XYMultipleSeriesDataset buildDateDataset(String[] titles, List<double[]> xValues, List<double[]> yValues)
    {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        
        int length = titles.length;
        for (int i = 0; i < length; i++)
        {
            TimeSeries series = new TimeSeries(titles[i]);
            double[] xV = xValues.get(i);
            double[] yV = yValues.get(i);
            int seriesLength = xV.length;
            for (int k = 0; k < seriesLength; k++)
            {
                series.add(xV[k], yV[k]);
            }
            dataset.addSeries(series);
        }
        
        return dataset;
    }

    /**
     * Builds an XY multiple series renderer.
     * 
     * @param colors
     *            the series rendering colors
     * @param styles
     *            the series point styles
     * @return the XY multiple series renderers
     */
    protected XYMultipleSeriesRenderer buildRenderer(int nbLines)
    {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        setRenderer(renderer, nbLines);
        return renderer;
    }

    protected void setRenderer(XYMultipleSeriesRenderer renderer, int nbLines)
    {
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setPointSize(5f);
        renderer.setMargins(new int[] { 20, 30, 15, 20 });

        Random rand = new Random();
        
        for (int i = 0; i < nbLines; i++)
        {
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(Color.rgb(rand.nextInt(156) + 100, rand.nextInt(156) + 100, rand.nextInt(156) + 100));
            r.setPointStyle(PointStyle.POINT);
            renderer.addSeriesRenderer(r);
        }
    }

    /**
     * Sets a few of the series renderer settings.
     * 
     * @param renderer
     *            the renderer to set the properties to
     * @param title
     *            the chart title
     * @param xTitle
     *            the title for the X axis
     * @param yTitle
     *            the title for the Y axis
     * @param xMin
     *            the minimum value on the X axis
     * @param xMax
     *            the maximum value on the X axis
     * @param yMin
     *            the minimum value on the Y axis
     * @param yMax
     *            the maximum value on the Y axis
     * @param axesColor
     *            the axes color
     * @param labelsColor
     *            the labels color
     */
    protected void setChartSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle, String yTitle, double xMin, double xMax, double yMin, double yMax, int axesColor, int labelsColor)
    {
        renderer.setChartTitle(title);
        renderer.setXTitle(xTitle);
        renderer.setYTitle(yTitle);
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
        renderer.setAxesColor(axesColor);
        renderer.setLabelsColor(labelsColor);
    }
    
    /**
     *
     */
    private class readLogFileInBackground extends AsyncTask<Void, Void, Void>
    {
        private ProgressDialog dialog = new ProgressDialog(ViewDatalogActivity.this);
        
        /**
         * 
         */
        protected void onPreExecute()
        {
            dialog.setMessage("Reading datalog");
            dialog.show();
        }
        
        /**
         * 
         * @param result
         */
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            
            generateGraph();
            dialog.dismiss();
        }

        /**
         * 
         * @param params
         */
        @Override
        protected Void doInBackground(Void... params)
        {
            
            
            Bundle b = getIntent().getExtras();
            String datalog = b.getString("datalog");
            
            readDatalog(datalog);            
            
            return null;
        }
    }
}
