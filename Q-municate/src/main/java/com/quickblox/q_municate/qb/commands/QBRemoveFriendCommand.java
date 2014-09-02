package com.quickblox.q_municate.qb.commands;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.quickblox.internal.module.custom.request.QBCustomObjectRequestBuilder;
import com.quickblox.module.custom.QBCustomObjects;
import com.quickblox.module.custom.model.QBCustomObject;
import com.quickblox.q_municate.core.command.ServiceCommand;
import com.quickblox.q_municate.model.AppSession;
import com.quickblox.q_municate.model.User;
import com.quickblox.q_municate.service.QBService;
import com.quickblox.q_municate.service.QBServiceConsts;
import com.quickblox.q_municate.utils.Consts;

import java.util.List;

public class QBRemoveFriendCommand extends ServiceCommand {

    private static final String TAG = QBRemoveFriendCommand.class.getSimpleName();

    public QBRemoveFriendCommand(Context context, String successAction, String failAction) {
        super(context, successAction, failAction);
    }

    public static void start(Context context, User friend) {
        Intent intent = new Intent(QBServiceConsts.REMOVE_FRIEND_ACTION, null, context, QBService.class);
        intent.putExtra(QBServiceConsts.EXTRA_FRIEND, friend);
        context.startService(intent);
    }

    @Override
    protected Bundle perform(Bundle extras) throws Exception {
        //TODO VF Implementation will be changed
        User friend = (User) extras.getSerializable(QBServiceConsts.EXTRA_FRIEND);

        QBCustomObjectRequestBuilder builder = new QBCustomObjectRequestBuilder();
        builder.eq(Consts.FRIEND_FIELD_USER_ID, AppSession.getSession().getUser().getId());
        builder.eq(Consts.FRIEND_FIELD_FRIEND_ID, friend.getUserId());

        List<QBCustomObject> objects = QBCustomObjects.getObjects(Consts.EXTRA_FRIEND, builder);

        QBCustomObjects.deleteObject(Consts.EXTRA_FRIEND, objects.get(0).getCustomObjectId());

        return null;
    }
}
