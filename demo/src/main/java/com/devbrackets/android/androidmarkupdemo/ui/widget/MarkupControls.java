package com.devbrackets.android.androidmarkupdemo.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.devbrackets.android.androidmarkup.widget.MarkupEditText;
import com.devbrackets.android.androidmarkupdemo.R;

/**
 * A simple widget to perform the controls for interacting with the
 * {@link MarkupEditText}
 */
public class MarkupControls extends FrameLayout implements MarkupEditText.MarkupControlsCallbacks {

    public interface Callback {
        boolean onBoldClick();
        boolean onItalicClick();
        boolean onUnorderedListClick();
        boolean onOrderedListClick();
    }

    protected ImageView boldView;
    protected ImageView italicView;
    protected ImageView unorderedListView;
    protected ImageView orderedListView;

    @Nullable
    private Callback callback;
    @Nullable
    private MarkupEditText markupEditText;

    private Context context;

    public MarkupControls(Context context) {
        super(context);
        init(context, null);
    }

    public MarkupControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MarkupControls(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MarkupControls(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @Override
    public void boldToggled(boolean on) {
        if (on) {
            boldView.setBackgroundColor(ContextCompat.getColor(context, R.color.grey));
        } else {
            boldView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }
    }

    @Override
    public void italicToggled(boolean on) {
        if (on) {
            italicView.setBackgroundColor(ContextCompat.getColor(context, R.color.grey));
        } else {
            italicView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }
    }

    @Override
    public void orderedListToggled(boolean on) {
        if (on) {
            orderedListView.setBackgroundColor(ContextCompat.getColor(context, R.color.grey));
        } else {
            orderedListView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }
    }

    @Override
    public void unOrderedListToggled(boolean on) {
        if (on) {
            unorderedListView.setBackgroundColor(ContextCompat.getColor(context, R.color.grey));
        } else {
            unorderedListView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setMarkupEditText(@Nullable MarkupEditText markupEditText) {
        this.markupEditText = markupEditText;
        if (this.markupEditText != null) {
            this.markupEditText.markupControlsCallbacks = this;
        }
    }

    /**
     * Initializes the playback controls by setting the layout, preparing helpers, and
     * linking views and callbacks.
     *
     * @param context The context for the owner of the playback controls
     */
    private void init(Context context, @Nullable AttributeSet attrs) {
        this.context = context;
        View.inflate(context, R.layout.widget_markup_controls, this);

        //Done to support the xml layout preview
        if (isInEditMode()) {
            return;
        }

        retrieveViews();
        setupListeners();
    }

    private void retrieveViews() {
        boldView = (ImageView)findViewById(R.id.markup_controls_bold);
        italicView = (ImageView)findViewById(R.id.markup_controls_italic);
        orderedListView = (ImageView)findViewById(R.id.markup_controls_ordered_list);
        unorderedListView = (ImageView)findViewById(R.id.markup_controls_unordered_list);
    }

    private void setupListeners() {
        boldView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBoldClick();
            }
        });

        italicView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onItalicClick();
            }
        });

        orderedListView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onOrderedListClick();
            }
        });

        unorderedListView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onUnorderedListClick();
            }
        });
    }

    private void onBoldClick() {
        if ((callback == null || !callback.onBoldClick()) && markupEditText != null) {
            markupEditText.toggleBold();
        }
    }

    private void onItalicClick() {
        if ((callback == null || !callback.onItalicClick()) && markupEditText != null) {
            markupEditText.toggleItalics();
        }
    }

    private void onUnorderedListClick() {
        if ((callback == null || !callback.onUnorderedListClick()) && markupEditText != null) {
            markupEditText.toggleUnOrderedList();
        }
    }

    private void onOrderedListClick() {
        if ((callback == null || !callback.onOrderedListClick()) && markupEditText != null) {
            markupEditText.toggleOrderedList();
        }
    }
}
