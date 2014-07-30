package com.quickblox.qmunicate.ui.chats;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.quickblox.module.chat.model.QBDialog;
import com.quickblox.qmunicate.R;
import com.quickblox.qmunicate.caching.tables.DialogMessageTable;
import com.quickblox.qmunicate.ui.base.BaseCursorAdapter;
import com.quickblox.qmunicate.ui.views.RoundedImageView;
import com.quickblox.qmunicate.utils.Consts;
import com.quickblox.qmunicate.utils.ImageHelper;
import com.quickblox.qmunicate.utils.ReceiveFileListener;
import com.quickblox.qmunicate.utils.ReceiveImageFileTask;
import com.quickblox.qmunicate.utils.ReceiveMaskedBitmapListener;
import com.quickblox.qmunicate.utils.ReceiveMaskedImageFileTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BaseDialogMessagesAdapter extends BaseCursorAdapter implements ReceiveFileListener {

    protected ScrollMessagesListener scrollMessagesListener;
    protected ImageHelper imageHelper;
    protected QBDialog dialog;

    private final int colorMaxValue = 255;
    private final float colorAlpha = 0.8f;

    private Random random;
    private Map<Integer, Integer> colorsMap;

    public BaseDialogMessagesAdapter(Context context, Cursor cursor) {
        super(context, cursor, true);
        random = new Random();
        colorsMap = new HashMap<Integer, Integer>();
        imageHelper = new ImageHelper((android.app.Activity) context);
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getItemViewType(cursor);
    }

    @Override
    public int getViewTypeCount() {
        return Consts.MESSAGES_TYPE_COUNT;
    }

    protected boolean isOwnMessage(int senderId) {
        return senderId == currentUser.getId();
    }

    protected void displayAttachImage(String attachUrl, final ViewHolder viewHolder, int maskedBackgroundId) {
        ImageLoader.getInstance().displayImage(attachUrl, viewHolder.attachImageView, Consts.UIL_DEFAULT_DISPLAY_OPTIONS,
                new SimpleImageLoading(viewHolder, maskedBackgroundId),
                new SimpleImageLoadingProgressListener(viewHolder));
    }

    protected int getTextColor(Integer senderId) {
        if (colorsMap.get(senderId) != null) {
            return colorsMap.get(senderId);
        } else {
            int colorValue = getRandomColor();
            colorsMap.put(senderId, colorValue);
            return colorsMap.get(senderId);
        }
    }

    protected int getMaskedImageBackgroundId(int senderId) {
        int maskedBackgroundId;
        if (isOwnMessage(senderId)) {
            maskedBackgroundId = R.drawable.right_bubble;
        } else {
            maskedBackgroundId = R.drawable.left_bubble;
        }
        return maskedBackgroundId;
    }

    protected void resetUI(ViewHolder viewHolder) {
        viewHolder.attachMessageRelativeLayout.setVisibility(View.GONE);
        viewHolder.progressRelativeLayout.setVisibility(View.GONE);
        viewHolder.textMessageView.setVisibility(View.GONE);
    }

    private int getRandomColor() {
        float[] hsv = new float[3];
        int color = Color.argb(colorMaxValue, random.nextInt(colorMaxValue), random.nextInt(colorMaxValue),
                random.nextInt(colorMaxValue));
        Color.colorToHSV(color, hsv);
        hsv[2] *= colorAlpha;
        color = Color.HSVToColor(hsv);
        return color;
    }

    private int getItemViewType(Cursor cursor) {
        int senderId = cursor.getInt(cursor.getColumnIndex(DialogMessageTable.Cols.SENDER_ID));
        if (isOwnMessage(senderId)) {
            return Consts.OWN_DIALOG_MESSAGE_TYPE;
        } else {
            return Consts.OPPONENT_DIALOG_MESSAGE_TYPE;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void hideAttachmentBackground(ImageView attachImageView) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            attachImageView.setBackgroundDrawable(null);
        } else {
            attachImageView.setBackground(null);
        }
    }

    @Override
    public void onCachedImageFileReceived(File imageFile) {
    }

    @Override
    public void onAbsolutePathExtFileReceived(String absolutePath) {
        imageHelper.showFullImage(context, absolutePath);
    }

    public class SimpleImageLoading extends SimpleImageLoadingListener implements ReceiveMaskedBitmapListener {

        private ViewHolder viewHolder;
        private int maskedBackgroundId;
        private Bitmap loadedImageBitmap;

        public SimpleImageLoading(ViewHolder viewHolder, int maskedBackgroundId) {
            this.viewHolder = viewHolder;
            this.maskedBackgroundId = maskedBackgroundId;
        }

        @Override
        public void onMaskedImageBitmapReceived(Bitmap maskedImageBitmap) {
            hideAttachmentBackground(viewHolder.attachImageView);
            viewHolder.attachImageView.setImageBitmap(maskedImageBitmap);
            viewHolder.attachMessageRelativeLayout.setVisibility(View.VISIBLE);
            viewHolder.attachImageView.setVisibility(View.VISIBLE);
            updateUIAfterLoading();
            scrollMessagesListener.onScrollToBottom();
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            updateUIAfterLoading();
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, final Bitmap loadedBitmap) {
            initMaskedImageView(loadedBitmap);
        }

        private void initMaskedImageView(Bitmap loadedBitmap) {
            loadedImageBitmap = loadedBitmap;
            makeMaskedImageView();
            viewHolder.attachImageView.setOnClickListener(receiveImageFileOnClickListener());
        }

        private void updateUIAfterLoading() {
            if (viewHolder.progressRelativeLayout != null) {
                viewHolder.progressRelativeLayout.setVisibility(View.GONE);
            }
        }

        private View.OnClickListener receiveImageFileOnClickListener() {
            return new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.chat_attached_file_click));
                    new ReceiveImageFileTask(BaseDialogMessagesAdapter.this).execute(imageHelper,
                            loadedImageBitmap, false);
                }
            };
        }

        private void makeMaskedImageView() {
            new ReceiveMaskedImageFileTask(this).execute(imageHelper, maskedBackgroundId, loadedImageBitmap);
        }
    }

    public class SimpleImageLoadingProgressListener implements ImageLoadingProgressListener {

        private ViewHolder viewHolder;

        public SimpleImageLoadingProgressListener(ViewHolder viewHolder) {
            this.viewHolder = viewHolder;
        }

        @Override
        public void onProgressUpdate(String imageUri, View view, int current, int total) {
            viewHolder.verticalProgressBar.setProgress(Math.round(100.0f * current / total));
        }
    }

    public class ViewHolder {
        public RoundedImageView avatarImageView;
        public TextView nameTextView;
        public View textMessageView;
        public RelativeLayout progressRelativeLayout;
        public RelativeLayout attachMessageRelativeLayout;
        public TextView messageTextView;
        public ImageView attachImageView;
        public TextView timeTextMessageTextView;
        public TextView timeAttachMessageTextView;
        public ProgressBar verticalProgressBar;
        public ProgressBar centeredProgressBar;
    }
}