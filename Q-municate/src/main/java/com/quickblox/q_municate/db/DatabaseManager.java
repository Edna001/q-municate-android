package com.quickblox.q_municate.db;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.text.TextUtils;

import com.quickblox.module.chat.QBHistoryMessage;
import com.quickblox.module.chat.model.QBDialog;
import com.quickblox.module.chat.model.QBDialogType;
import com.quickblox.q_municate.R;
import com.quickblox.q_municate.db.tables.DialogTable;
import com.quickblox.q_municate.db.tables.FriendTable;
import com.quickblox.q_municate.db.tables.FriendsRelationTable;
import com.quickblox.q_municate.db.tables.MessageTable;
import com.quickblox.q_municate.db.tables.UserTable;
import com.quickblox.q_municate.model.Friend;
import com.quickblox.q_municate.model.MessageCache;
import com.quickblox.q_municate.model.User;
import com.quickblox.q_municate.utils.ChatUtils;
import com.quickblox.q_municate.utils.Consts;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DatabaseManager {

    public static void savePeople(Context context, List<User> usersList, List<Friend> friendsList) {
        for (User user : usersList) {
            saveUser(context, user);
        }
        for (Friend friend : friendsList) {
            saveFriend(context, friend);
        }
    }

    public static void saveUser(Context context, User user) {
        ContentValues values = getContentValuesUserTable(user);

        String condition = UserTable.Cols.USER_ID + "='" + user.getUserId() + "'";
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(UserTable.CONTENT_URI, null, condition, null, null);

        if (cursor != null && cursor.getCount() > Consts.ZERO_INT_VALUE) {
            resolver.update(UserTable.CONTENT_URI, values, condition, null);
        } else {
            resolver.insert(UserTable.CONTENT_URI, values);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    public static void saveFriend(Context context, Friend friend) {
        ContentValues values = getContentValuesFriendTable(context, friend);

        String condition = FriendTable.Cols.USER_ID + "='" + friend.getUserId() + "'";
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(FriendTable.CONTENT_URI, null, condition, null, null);

        if (cursor != null && cursor.getCount() > Consts.ZERO_INT_VALUE) {
            resolver.update(FriendTable.CONTENT_URI, values, condition, null);
        } else {
            resolver.insert(FriendTable.CONTENT_URI, values);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private static ContentValues getContentValuesFriendTable(Context context, Friend friend) {
        ContentValues values = new ContentValues();

        int relationStatusId = getRelationStatusId(context, friend.getRelationStatus());
        if (relationStatusId == Consts.ZERO_INT_VALUE) {
            relationStatusId = saveFriendsRelationStatus(context, friend.getRelationStatus());
        }

        values.put(FriendTable.Cols.USER_ID, friend.getUserId());
        values.put(FriendTable.Cols.RELATION_STATUS_ID, relationStatusId);

        return values;
    }

    private static int saveFriendsRelationStatus(Context context, String relationStatus) {
        ContentValues values = new ContentValues();
        values.put(FriendsRelationTable.Cols.RELATION_STATUS, relationStatus);

        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(FriendsRelationTable.CONTENT_URI, null, null, null, null);

        resolver.insert(FriendsRelationTable.CONTENT_URI, values);

        int relationStatusId = Consts.ZERO_INT_VALUE;

        if(cursor != null && cursor.moveToFirst()) {
            relationStatusId = cursor.getInt(cursor.getColumnIndex(FriendsRelationTable.Cols.RELATION_STATUS_ID));
        }

        if (cursor != null) {
            cursor.close();
        }

        return relationStatusId;
    }

    public static int getRelationStatusId(Context context, String relationStatus) {
        int relationStatusId = Consts.ZERO_INT_VALUE;

        Cursor cursor = context.getContentResolver().query(FriendsRelationTable.CONTENT_URI, null,
                FriendsRelationTable.Cols.RELATION_STATUS + " = '" + relationStatus + "'", null, null);

        if(cursor != null && cursor.moveToFirst()) {
            relationStatusId = cursor.getInt(cursor.getColumnIndex(FriendsRelationTable.Cols.RELATION_STATUS_ID));
        }

        if (cursor != null) {
            cursor.close();
        }

        return relationStatusId;
    }

    public static boolean isFriendInBase(Context context, int searchId) {
        Cursor cursor = context.getContentResolver().query(UserTable.CONTENT_URI, null,
                UserTable.Cols.USER_ID + " = " + searchId, null, null);

        boolean isFriendInBase = cursor.getCount() > Consts.ZERO_INT_VALUE;

        cursor.close();

        return isFriendInBase;
    }

    public static Cursor getFriendsByFullName(Context context, String fullName) {
        Cursor cursor;
        String sorting = UserTable.Cols.ID + " ORDER BY " + UserTable.Cols.FULL_NAME + " COLLATE NOCASE ASC";
        if (TextUtils.isEmpty(fullName)) {
            cursor = context.getContentResolver().query(UserTable.CONTENT_URI, null, null, null, sorting);
        } else {
            cursor = context.getContentResolver().query(UserTable.CONTENT_URI, null,
                    UserTable.Cols.FULL_NAME + " like '%" + fullName + "%'", null, sorting);
        }
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public static Cursor getAllFriends(Context context) {
        String condition = UserTable.TABLE_NAME + "." + UserTable.Cols.USER_ID + " = " + FriendTable.TABLE_NAME + "." + FriendTable.Cols.USER_ID;
        String sortOrder = UserTable.Cols.ID + " ORDER BY " + UserTable.Cols.FULL_NAME + " COLLATE NOCASE ASC";
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(UserTable.USER_TABLE_CONTENT_URI, null, condition, null, sortOrder);

        return cursor;

//        return context.getContentResolver().query(UserTable.CONTENT_URI, null, null, null,
//                UserTable.Cols.ID + " ORDER BY " + UserTable.Cols.FULL_NAME + " COLLATE NOCASE ASC");
    }

    public static List<User> getAllFriendsList(Context context) {
        List<User> friendList = new ArrayList<User>();
        Cursor cursor = getAllFriends(context);
        while (cursor.moveToNext()) {
            friendList.add(getFriendFromCursor(cursor));
        }
        cursor.close();
        return friendList;
    }

    public static Cursor getFriendsFilteredByIds(Context context, List<Integer> friendIdsList) {
        String selection = prepareFriendsFilter(friendIdsList);
        return context.getContentResolver().query(UserTable.CONTENT_URI, null, selection, null,
                UserTable.Cols.ID + " ORDER BY " + UserTable.Cols.FULL_NAME + " COLLATE NOCASE ASC");
    }

    private static String prepareFriendsFilter(List<Integer> friendIdsList) {
        String condition = String.format("('%s')", TextUtils.join("','", friendIdsList));
        return UserTable.Cols.USER_ID + " NOT IN " + condition;
    }

    public static void deleteAllFriends(Context context) {
        context.getContentResolver().delete(UserTable.CONTENT_URI, null, null);
    }

    public static void saveDialogs(Context context, List<QBDialog> dialogsList) {
        for (QBDialog dialog : dialogsList) {
            saveDialog(context, dialog);
        }
    }

    public static QBDialog getDialogByDialogId(Context context, String dialogId) {
        QBDialog dialog = null;
        Cursor cursor = context.getContentResolver().query(DialogTable.CONTENT_URI, null,
                DialogTable.Cols.DIALOG_ID + " = '" + dialogId + "'", null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String resultDialogId = cursor.getString(cursor.getColumnIndex(DialogTable.Cols.DIALOG_ID));
            if (!TextUtils.isEmpty(resultDialogId)) {
                dialog = getDialogFromCursor(cursor);
            }
            cursor.close();
        }

        return dialog;
    }

    public static QBDialog getDialogByRoomJid(Context context, String roomJid) {
        QBDialog dialog = null;
        Cursor cursor = context.getContentResolver().query(DialogTable.CONTENT_URI, null,
                DialogTable.Cols.ROOM_JID_ID + " = '" + roomJid + "'", null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String resultDialogId = cursor.getString(cursor.getColumnIndex(DialogTable.Cols.DIALOG_ID));
            if (!TextUtils.isEmpty(resultDialogId)) {
                dialog = getDialogFromCursor(cursor);
            }
            cursor.close();
        }

        return dialog;
    }

    public static List<QBDialog> getDialogsByOpponent(Context context, int opponent,
            QBDialogType dialogType) {
        List<QBDialog> dialogs = new LinkedList<QBDialog>();

        Cursor cursor = context.getContentResolver().query(DialogTable.CONTENT_URI, null,
                        DialogTable.Cols.TYPE + "= ? AND " + DialogTable.Cols.OCCUPANTS_IDS + " like ?",
                new String[]{ String.valueOf(dialogType.getCode()),  "%" + opponent + "%"}, null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                dialogs.add(getDialogFromCursor(cursor));
            }

            cursor.close();
        }

        return dialogs;
    }

    public static void saveDialog(Context context, QBDialog dialog) {
        ContentValues values;
        String condition = DialogTable.Cols.DIALOG_ID + "='" + dialog.getDialogId() + "'";
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(DialogTable.CONTENT_URI, null, condition, null, null);
        if (cursor != null && cursor.getCount() > Consts.ZERO_INT_VALUE) {
            values = getContentValuesForUpdateDialogTable(context, dialog);
            resolver.update(DialogTable.CONTENT_URI, values, condition, null);
        } else {
            values = getContentValuesForCreateDialogTable(context, dialog);
            resolver.insert(DialogTable.CONTENT_URI, values);
        }
        cursor.close();
    }

    public static Cursor getAllDialogs(Context context) {
        return context.getContentResolver().query(DialogTable.CONTENT_URI, null, null, null,
                DialogTable.Cols.ID + " ORDER BY " + DialogTable.Cols.LAST_DATE_SENT + " DESC");
    }

    public static List<QBDialog> getDialogs(Context context) {
        Cursor allDialogsCursor = getAllDialogs(context);
        List<QBDialog> dialogs = new ArrayList<QBDialog>(allDialogsCursor.getCount());
        if (allDialogsCursor != null) {
            if (allDialogsCursor.getCount() > Consts.ZERO_INT_VALUE) {
                while (allDialogsCursor.moveToNext()) {
                    dialogs.add(getDialogFromCursor(allDialogsCursor));
                }
            }
            allDialogsCursor.close();
        }
        return dialogs;
    }

    public static MessageCache getMessageCacheFromCursor(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(MessageTable.Cols.ID));
        String dialogId = cursor.getString(cursor.getColumnIndex(MessageTable.Cols.DIALOG_ID));
        String packetId = cursor.getString(cursor.getColumnIndex(MessageTable.Cols.PACKET_ID));
        Integer senderId = cursor.getInt(cursor.getColumnIndex(MessageTable.Cols.SENDER_ID));
        String body = cursor.getString(cursor.getColumnIndex(MessageTable.Cols.BODY));
        long time = cursor.getLong(cursor.getColumnIndex(MessageTable.Cols.TIME));
        String attachUrl = cursor.getString(cursor.getColumnIndex(MessageTable.Cols.ATTACH_FILE_ID));
        boolean isRead = cursor.getInt(cursor.getColumnIndex(MessageTable.Cols.IS_READ)) > Consts.ZERO_INT_VALUE;
        boolean isDelivered = cursor.getInt(cursor.getColumnIndex(MessageTable.Cols.IS_DELIVERED)) > Consts.ZERO_INT_VALUE;

        MessageCache messageCache = new MessageCache(id, dialogId, packetId, senderId, body, attachUrl, time, isRead, isDelivered);

        return messageCache;
    }

    public static QBDialog getDialogFromCursor(Cursor cursor) {
        String dialogId = cursor.getString(cursor.getColumnIndex(DialogTable.Cols.DIALOG_ID));
        String roomJidId = cursor.getString(cursor.getColumnIndex(DialogTable.Cols.ROOM_JID_ID));
        String name = cursor.getString(cursor.getColumnIndex(DialogTable.Cols.NAME));
        String photoUrl = cursor.getString(cursor.getColumnIndex(DialogTable.Cols.PHOTO_URL));
        String occupantsIdsString = cursor.getString(cursor.getColumnIndex(DialogTable.Cols.OCCUPANTS_IDS));
        ArrayList<Integer> occupantsIdsList = ChatUtils.getOccupantsIdsListFromString(occupantsIdsString);
        int countUnreadMessages = cursor.getInt(cursor.getColumnIndex(
                DialogTable.Cols.COUNT_UNREAD_MESSAGES));
        String lastMessage = cursor.getString(cursor.getColumnIndex(DialogTable.Cols.LAST_MESSAGE));
        Integer lastMessageUserId = cursor.getInt(cursor.getColumnIndex(
                DialogTable.Cols.LAST_MESSAGE_USER_ID));
        long dateSent = cursor.getLong(cursor.getColumnIndex(DialogTable.Cols.LAST_DATE_SENT));
        int type = cursor.getInt(cursor.getColumnIndex(DialogTable.Cols.TYPE));

        QBDialog dialog = new QBDialog(dialogId);
        dialog.setRoomJid(roomJidId);
        dialog.setName(name);
        dialog.setOccupantsIds(occupantsIdsList);
        dialog.setUnreadMessageCount(countUnreadMessages);
        dialog.setLastMessage(lastMessage);
        dialog.setLastMessageUserId(lastMessageUserId);
        dialog.setLastMessageDateSent(dateSent);
        dialog.setPhotoUrl(photoUrl);
        dialog.setType(QBDialogType.parseByCode(type));

        return dialog;
    }

    public static User getFriendById(Context context, int friendId) {
        Cursor cursor = context.getContentResolver().query(UserTable.CONTENT_URI, null,
                UserTable.Cols.USER_ID + " = " + friendId, null, null);
        User friend = null;
        if (cursor != null && cursor.moveToFirst()) {
            friend = getFriendFromCursor(cursor);
            cursor.close();
        }
        return friend;
    }

    public static Cursor getFriendCursorById(Context context, int friendId) {
        Cursor cursor = context.getContentResolver().query(UserTable.CONTENT_URI, null,
                UserTable.Cols.USER_ID + " = " + friendId, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public static User getFriendFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(UserTable.Cols.USER_ID));
        String fullName = cursor.getString(cursor.getColumnIndex(UserTable.Cols.FULL_NAME));
        String email = cursor.getString(cursor.getColumnIndex(UserTable.Cols.EMAIL));
        String phone = cursor.getString(cursor.getColumnIndex(UserTable.Cols.PHONE));
        String avatarUid = cursor.getString(cursor.getColumnIndex(UserTable.Cols.AVATAR_URL));
        String status = cursor.getString(cursor.getColumnIndex(UserTable.Cols.STATUS));
        boolean online = cursor.getInt(cursor.getColumnIndex(UserTable.Cols.IS_ONLINE)) > 0;
//        String type = cursor.getString(cursor.getColumnIndex(UserTable.Cols.TYPE));

        User friend = new User(id, fullName, email, phone, avatarUid);
        friend.setStatus(status);
        friend.setOnline(online);

        return friend;
    }

    public static MessageCache getLastReadMessage(Context context, QBDialog dialog) {
        MessageCache messageCache = null;

        Cursor cursor = context.getContentResolver().query(
                MessageTable.CONTENT_URI,
                null,
                MessageTable.Cols.DIALOG_ID + " = '" + dialog.getDialogId() + "' AND " +
                MessageTable.Cols.IS_READ + " > 0",
                null,
                MessageTable.Cols.ID + " ORDER BY " + MessageTable.Cols.TIME + " COLLATE NOCASE ASC");

        if (cursor != null && cursor.getCount() > Consts.ZERO_INT_VALUE) {
            cursor.moveToLast();
            messageCache = getMessageCacheFromCursor(cursor);
        }

        return messageCache;
    }

    public static int getCountUnreadDialogs(Context context) {
        Cursor cursor = context.getContentResolver().query(DialogTable.CONTENT_URI, null,
                DialogTable.Cols.COUNT_UNREAD_MESSAGES + " > 0", null, null);
        return cursor.getCount();
    }

    public static Cursor getAllDialogMessagesByDialogId(Context context, String dialogId) {
        return context.getContentResolver().query(MessageTable.CONTENT_URI, null,
                MessageTable.Cols.DIALOG_ID + " = '" + dialogId + "'", null,
                MessageTable.Cols.ID + " ORDER BY " + MessageTable.Cols.TIME + " COLLATE NOCASE ASC");
    }

    public static void deleteAllMessages(Context context) {
        context.getContentResolver().delete(MessageTable.CONTENT_URI, null, null);
    }

    public static void deleteAllDialogs(Context context) {
        context.getContentResolver().delete(DialogTable.CONTENT_URI, null, null);
    }

    public static void saveChatMessages(Context context, List<QBHistoryMessage> messagesList,
            String dialogId) {
        for (QBHistoryMessage historyMessage : messagesList) {
            String messageId = historyMessage.getMessageId();
            String message = historyMessage.getBody();
            int senderId = historyMessage.getSenderId();
            long time = historyMessage.getDateSent();

            String attachURL;

            attachURL = ChatUtils.getAttachUrlFromMessage(historyMessage.getAttachments());

            MessageCache messageCache = new MessageCache(messageId, dialogId, null, senderId, message,
                    attachURL, time, true, true);

            saveChatMessage(context, messageCache);
        }
    }

    public static void saveChatMessage(Context context, MessageCache messageCache) {
        ContentValues values = new ContentValues();
        values.put(MessageTable.Cols.ID, messageCache.getId());
        values.put(MessageTable.Cols.DIALOG_ID, messageCache.getDialogId());
        values.put(MessageTable.Cols.PACKET_ID, messageCache.getPacketId());
        values.put(MessageTable.Cols.SENDER_ID, messageCache.getSenderId());
        String body = messageCache.getMessage();
        if (TextUtils.isEmpty(body)) {
            values.put(MessageTable.Cols.BODY, body);
        } else {
            values.put(MessageTable.Cols.BODY, Html.fromHtml(body).toString());
        }
        values.put(MessageTable.Cols.TIME, messageCache.getTime());
        values.put(MessageTable.Cols.ATTACH_FILE_ID, messageCache.getAttachUrl());
        values.put(MessageTable.Cols.IS_READ, messageCache.isRead());
        values.put(MessageTable.Cols.IS_DELIVERED, messageCache.isDelivered());
        context.getContentResolver().insert(MessageTable.CONTENT_URI, values);

        updateDialog(context, messageCache.getDialogId(), messageCache.getMessage(),
                messageCache.getTime(), messageCache.getSenderId());
    }

    public static boolean isExistDialogById(Context context, String dialogId) {
        Cursor cursor = context.getContentResolver().query(DialogTable.CONTENT_URI, null,
                DialogTable.Cols.DIALOG_ID + " = '" + dialogId + "'", null, null);

        if (cursor != null && cursor.getCount() > Consts.ZERO_INT_VALUE) {
            cursor.close();
            return true;
        }
        return false;
    }

    public static void deleteMessagesByDialogId(Context context, String dialogId) {
        context.getContentResolver().delete(MessageTable.CONTENT_URI,
                MessageTable.Cols.DIALOG_ID + " = '" + dialogId + "'", null);
    }

    private static ContentValues getContentValuesUserTable(User friend) {
        ContentValues values = new ContentValues();
        values.put(UserTable.Cols.USER_ID, friend.getUserId());
        values.put(UserTable.Cols.FULL_NAME, friend.getFullName());
        values.put(UserTable.Cols.EMAIL, friend.getEmail());
        values.put(UserTable.Cols.PHONE, friend.getPhone());
        values.put(UserTable.Cols.AVATAR_URL, friend.getAvatarUrl());
        values.put(UserTable.Cols.STATUS, friend.getOnlineStatus());
        values.put(UserTable.Cols.IS_ONLINE, friend.isOnline());
//        values.put(UserTable.Cols.TYPE, friend.getType().name());
        return values;
    }

    private static ContentValues getContentValuesForUpdateDialogTable(Context context, QBDialog dialog) {
        ContentValues values = new ContentValues();
        if (!TextUtils.isEmpty(dialog.getDialogId())) {
            values.put(DialogTable.Cols.DIALOG_ID, dialog.getDialogId());
        }
        values.put(DialogTable.Cols.NAME, dialog.getName());
        values.put(DialogTable.Cols.PHOTO_URL, dialog.getPhotoUrl());
        values.put(DialogTable.Cols.OCCUPANTS_IDS, ChatUtils.getOccupantsIdsStringFromList(
                dialog.getOccupants()));
        String bodyLastMessage = getLastMessage(context, dialog.getLastMessage(), dialog.getLastMessageDateSent());
        if (TextUtils.isEmpty(bodyLastMessage)) {
            values.put(DialogTable.Cols.LAST_MESSAGE, bodyLastMessage);
        } else {
            values.put(DialogTable.Cols.LAST_MESSAGE, Html.fromHtml(bodyLastMessage).toString());
        }
        values.put(DialogTable.Cols.LAST_DATE_SENT, dialog.getLastMessageDateSent());
        values.put(DialogTable.Cols.COUNT_UNREAD_MESSAGES, dialog.getUnreadMessageCount());
        return values;
    }

    private static ContentValues getContentValuesForCreateDialogTable(Context context, QBDialog dialog) {
        ContentValues values = new ContentValues();
        values.put(DialogTable.Cols.DIALOG_ID, dialog.getDialogId());
        values.put(DialogTable.Cols.ROOM_JID_ID, dialog.getRoomJid());
        values.put(DialogTable.Cols.NAME, dialog.getName());
        values.put(DialogTable.Cols.COUNT_UNREAD_MESSAGES, dialog.getUnreadMessageCount());
        String bodyLastMessage = getLastMessage(context, dialog.getLastMessage(), dialog.getLastMessageDateSent());
        if (TextUtils.isEmpty(bodyLastMessage)) {
            values.put(DialogTable.Cols.LAST_MESSAGE, bodyLastMessage);
        } else {
            values.put(DialogTable.Cols.LAST_MESSAGE, Html.fromHtml(bodyLastMessage).toString());
        }
        values.put(DialogTable.Cols.LAST_MESSAGE_USER_ID, dialog.getLastMessageUserId());
        values.put(DialogTable.Cols.LAST_DATE_SENT, dialog.getLastMessageDateSent());
        String occupantsIdsString = ChatUtils.getOccupantsIdsStringFromList(dialog.getOccupants());
        values.put(DialogTable.Cols.PHOTO_URL, dialog.getPhotoUrl());
        values.put(DialogTable.Cols.OCCUPANTS_IDS, occupantsIdsString);
        values.put(DialogTable.Cols.TYPE, dialog.getType().getCode());
        return values;
    }

    private static String getLastMessage(Context context, String lastMessage, long lastDateSent) {
        return (TextUtils.isEmpty(lastMessage) && lastDateSent != Consts.ZERO_INT_VALUE) ? context.getString(
                R.string.dlg_attached_last_message) : lastMessage;
    }

    public static void updateDialog(Context context, String dialogId, String lastMessage, long dateSent,
            long lastSenderId) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(DialogTable.Cols.COUNT_UNREAD_MESSAGES, getCountUnreadMessagesByRoomJid(context,
                dialogId));
        String bodyLastMessage = getLastMessage(context, lastMessage, dateSent);
        if (TextUtils.isEmpty(bodyLastMessage)) {
            values.put(DialogTable.Cols.LAST_MESSAGE, bodyLastMessage);
        } else {
            values.put(DialogTable.Cols.LAST_MESSAGE, Html.fromHtml(bodyLastMessage).toString());
        }
        values.put(DialogTable.Cols.LAST_MESSAGE_USER_ID, lastSenderId);
        values.put(DialogTable.Cols.LAST_DATE_SENT, dateSent);
        String condition = DialogTable.Cols.DIALOG_ID + "='" + dialogId + "'";
        resolver.update(DialogTable.CONTENT_URI, values, condition, null);
    }

    public static int getCountUnreadMessagesByRoomJid(Context context, String dialogId) {
        Cursor cursor = context.getContentResolver().query(MessageTable.CONTENT_URI, null,
                MessageTable.Cols.IS_READ + " = 0 AND " + MessageTable.Cols.DIALOG_ID + " = '" + dialogId + "'",
                null, null);
        int countMessages = cursor.getCount();
        cursor.close();
        return countMessages;
    }

    public static void clearAllCache(Context context) {
        DatabaseManager.deleteAllFriends(context);
        DatabaseManager.deleteAllMessages(context);
        DatabaseManager.deleteAllDialogs(context);
        // TODO SF clear something else
    }

    public static void updateStatusMessage(Context context, String messageId, boolean isRead) {
        ContentValues values = new ContentValues();
        String condition = MessageTable.Cols.ID + "='" + messageId + "'";
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(MessageTable.CONTENT_URI, null, condition, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String roomJidId = cursor.getString(cursor.getColumnIndex(MessageTable.Cols.DIALOG_ID));
            String message = cursor.getString(cursor.getColumnIndex(MessageTable.Cols.BODY));
            long time = cursor.getLong(cursor.getColumnIndex(MessageTable.Cols.TIME));
            long lastSenderId = cursor.getLong(cursor.getColumnIndex(MessageTable.Cols.SENDER_ID));
            values.put(MessageTable.Cols.IS_READ, isRead);
            resolver.update(MessageTable.CONTENT_URI, values, condition, null);
            cursor.close();
            updateDialog(context, roomJidId, message, time, lastSenderId);
        }
    }

    public static void updateMessageDeliveryStatus(Context context, String messageId, boolean isDelivered) {
        ContentValues values = new ContentValues();
        String condition = MessageTable.Cols.ID + "='" + messageId + "'";
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(MessageTable.CONTENT_URI, null, condition, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            values.put(MessageTable.Cols.IS_DELIVERED, isDelivered);
            resolver.update(MessageTable.CONTENT_URI, values, condition, null);
            cursor.close();
        }
    }

    public static void deleteDialogByRoomJid(Context context, String roomJid) {
        context.getContentResolver().delete(DialogTable.CONTENT_URI,
                DialogTable.Cols.ROOM_JID_ID + " = '" + roomJid + "'", null);
    }
}