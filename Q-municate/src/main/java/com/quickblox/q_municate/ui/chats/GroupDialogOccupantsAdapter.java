package com.quickblox.q_municate.ui.chats;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.quickblox.q_municate.R;
import com.quickblox.q_municate.model.User;
import com.quickblox.q_municate.ui.base.BaseActivity;
import com.quickblox.q_municate.ui.base.BaseListAdapter;
import com.quickblox.q_municate.ui.views.RoundedImageView;

import java.util.List;

public class GroupDialogOccupantsAdapter extends BaseListAdapter<User> {

    public GroupDialogOccupantsAdapter(BaseActivity baseActivity, List<User> objectsList) {
        super(baseActivity, objectsList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        User friend = getItem(position);

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_item_dialog_friend, null);
            holder = new ViewHolder();

            holder.avatarImageView = (RoundedImageView) convertView.findViewById(R.id.avatar_imageview);
            holder.nameTextView = (TextView) convertView.findViewById(R.id.name_textview);
            holder.onlineImageView = (ImageView) convertView.findViewById(R.id.online_imageview);
            holder.onlineStatusMessageTextView = (TextView) convertView.findViewById(
                    R.id.statusMessageTextView);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String fullname;
        if (isFriend(friend)) {
            fullname = friend.getFullName();
            holder.onlineStatusMessageTextView.setVisibility(View.VISIBLE);
        } else {
            fullname = String.valueOf(friend.getUserId());
            holder.onlineStatusMessageTextView.setVisibility(View.GONE);
        }
        holder.nameTextView.setText(fullname);

        holder.onlineStatusMessageTextView.setText(friend.getOnlineStatus());
        if (friend.isOnline()) {
            holder.onlineImageView.setVisibility(View.VISIBLE);
        } else {
            holder.onlineImageView.setVisibility(View.GONE);
        }
        displayImage(friend.getAvatarUrl(), holder.avatarImageView);

        return convertView;
    }

    private boolean isFriend(User friend) {
        return friend.getFullName() != null;
    }

    private static class ViewHolder {

        RoundedImageView avatarImageView;
        TextView nameTextView;
        ImageView onlineImageView;
        TextView onlineStatusMessageTextView;
    }
}
