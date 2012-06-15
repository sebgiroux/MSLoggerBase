package uk.org.smithfamily.mslogger.widgets;

import java.text.NumberFormat;

import uk.org.smithfamily.mslogger.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * 
 */
public class NumericIndicator extends TextView implements Indicator
{
	private float			min;
	private float			max;
	private float			warningPoint;
	private float			errorPoint;
	private float			value;
//	private Typeface		font;
	private int				dp;
	private NumberFormat	formatter;
	private String			channel;
	private boolean			disabled;

	/**
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public NumericIndicator(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
//		setFont(context);
		setupDefaults(context);
	}

	/**
	 * 
	 * @param context
	 * @param attrs
	 */
	public NumericIndicator(Context context, AttributeSet attrs)
	{
		super(context, attrs);
//		setFont(context);
		setupDefaults(context);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Dial);
		value = a.getFloat(R.styleable.Dial_numValue, value);
		max = a.getFloat(R.styleable.Dial_scaleMaxValue, max);
		min = a.getFloat(R.styleable.Dial_scaleMinValue, min);
		warningPoint = a.getFloat(R.styleable.Dial_rangeWarningMinValue, warningPoint);
		errorPoint = a.getFloat(R.styleable.Dial_rangeErrorMinValue, errorPoint);
		channel = a.getString(R.styleable.Dial_channel);
		disabled = a.getBoolean(R.styleable.Dial_disabled, false);

		setupFormat();
	}

	/**
	 * 
	 * @param context
	 */
	public NumericIndicator(Context context)
	{
		super(context);
//		setFont(context);
		setupDefaults(context);
		setupFormat();
	}

	/**
	 * 
	 * @param context
	 */
	private void setupDefaults(Context context)
	{
		min = 10;
		max = 20;
		value = 14.7f;
		warningPoint = 16;
		errorPoint = 17;
	}
	
	/**
	 * 
	 */
	private void setupFormat()
	{
		double range = Math.abs(this.max - this.min);
		if (range <= 12)
			this.dp = 2;
		else if (range < 100)
			this.dp = 1;
		else
			this.dp = 0;

		if (dp == 0)
		{
			formatter = NumberFormat.getIntegerInstance();
		}
		else
		{
			formatter = NumberFormat.getNumberInstance();
		}

		formatter.setGroupingUsed(false);
		formatter.setMaximumFractionDigits(dp);
		formatter.setMinimumFractionDigits(dp);
		this.setText(formatter.format(this.value));
	}
/*
	private void setFont(Context context)
	{
		this.font = Typeface.createFromAsset(context.getAssets(), "fonts/digital_lcd.ttf");
		this.setTypeface(font);

	}
*/
	
	/**
	 * @param min
	 */
	public void setMin(float min)
	{
		this.min = min;
		setupFormat();

	}

	/**
	 * @param max
	 */
	public void setMax(float max)
	{
		this.max = max;
		setupFormat();

	}

	/**
	 * @param title
	 */
	public void setTitle(String title)
	{
		// No Op
	}

	/**
	 * @param value
	 */
	public void setCurrentValue(double value)
	{
		this.value = (float) value;
	}

	/**
	 * @param canvas
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		if (value < warningPoint)
			setTextColor(Color.GREEN);
		else if (value < errorPoint)
			setTextColor(Color.YELLOW);
		else
			setTextColor(Color.RED);
		if (disabled)
		{
			setTextColor(Color.LTGRAY);
			this.setText("---");
		}
		else
		{
			String val = formatter.format(this.value);
			this.setText(val);
		}
		super.onDraw(canvas);
	}

	/**
	 * @return 
	 */
	public String getChannel()
	{
		return channel;
	}

	/**
	 * @param channel
	 */
	public void setChannel(String channel)
	{
		this.channel = channel;
	}

	/**
	 * 
	 */
	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		IndicatorManager.INSTANCE.registerIndicator(this);

	}

	/**
	 * 
	 */
	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();

		IndicatorManager.INSTANCE.deregisterIndicator(this);

	}

	/**
	 * @param disabled
	 */
	@Override
	public void setDisabled(boolean disabled)
	{
		this.disabled = disabled;
		this.postInvalidate();
	}

	/**
	 * @param name
	 */
    @Override
    public void setName(String name)
    {
        // TODO Auto-generated method stub
    }

    /**
     * @param units
     */
    @Override
    public void setUnits(String units)
    {
        // TODO Auto-generated method stub
    }

    /**
     * @param lowD
     */
    @Override
    public void setLowD(float lowD)
    {
        // TODO Auto-generated method stub
    }

    /**
     * @param lowW
     */
    @Override
    public void setLowW(float lowW)
    {
        // TODO Auto-generated method stub      
    }

    /**
     * @param hiW
     */
    @Override
    public void setHiW(float hiW)
    {
        // TODO Auto-generated method stub     
    }

    /**
     * @param hiD
     */
    @Override
    public void setHiD(float hiD)
    {
        // TODO Auto-generated method stub  
    }

    /**
     * @param vd
     */
    @Override
    public void setVD(int vd)
    {
        // TODO Auto-generated method stub        
    }

    /**
     * @param ld
     */
    @Override
    public void setLD(int ld)
    {
        // TODO Auto-generated method stub       
    }
}