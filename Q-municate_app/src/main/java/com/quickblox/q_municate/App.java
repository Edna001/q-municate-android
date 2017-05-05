package com.quickblox.q_municate;

import android.content.Context;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.digits.sdk.android.Digits;
import com.j256.ormlite.logger.LocalLog;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.quickblox.auth.session.QBSessionListenerImpl;
import com.quickblox.auth.session.QBSessionManager;
import com.quickblox.auth.session.QBSessionParameters;
import com.quickblox.auth.session.QBSettings;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.ServiceZone;
import com.quickblox.q_municate.utils.StringObfuscator;
import com.quickblox.q_municate.utils.image.ImageLoaderUtils;
import com.quickblox.q_municate.utils.ActivityLifecycleHandler;
import com.quickblox.q_municate.utils.helpers.SharedHelper;
import com.quickblox.q_municate_auth_service.QMAuthService;
import com.quickblox.q_municate_core.models.AppSession;
import com.quickblox.q_municate_db.managers.DataManager;
import com.quickblox.q_municate_user_cache.QMUserCacheImpl;
import com.quickblox.q_municate_user_service.QMUserService;
import com.quickblox.q_municate_user_service.cache.QMUserCache;
import com.quickblox.users.model.QBUser;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;

import io.fabric.sdk.android.Fabric;

import static com.j256.ormlite.logger.LoggerFactory.LOG_TYPE_SYSTEM_PROPERTY;

public class App extends MultiDexApplication {

    private static final String TAG = App.class.getSimpleName();

    private final static String API_DOMAIN = "https://apistage1.quickblox.com";
    private final static String CHAT_DOMAIN = "chatstage1.quickblox.com";

    private static App instance;
    private SharedHelper appSharedHelper;



    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        Log.i(TAG, "onCreate with update");
        initFabric();
        initApplication();
        registerActivityLifecycleCallbacks(new ActivityLifecycleHandler());
    }

    private void initFabric(){
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        TwitterAuthConfig authConfig = new TwitterAuthConfig(
                StringObfuscator.getTwitterConsumerKey(),
                StringObfuscator.getTwitterConsumerSecret());

        Fabric.with(this,
                crashlyticsKit,
                new TwitterCore(authConfig),
                new Digits.Builder().withTheme(R.style.AppTheme).build());
    }

    private void initApplication() {
        instance = this;

        initQb();
        initDb();
        initImageLoader(this);
        initServices();
    }

    private void initQb() {
        QBSettings.getInstance().init(getApplicationContext(),
                StringObfuscator.getApplicationId(),
                StringObfuscator.getAuthKey(),
                StringObfuscator.getAuthSecret());
        QBSettings.getInstance().setAccountKey(StringObfuscator.getAccountKey());

        QBSettings.getInstance().setEndpoints(API_DOMAIN, CHAT_DOMAIN, ServiceZone.PRODUCTION);
                QBSettings.getInstance().setZone(ServiceZone.PRODUCTION);

        QBChatService.ConfigurationBuilder configurationBuilder = new QBChatService.ConfigurationBuilder();
        configurationBuilder.setAutojoinEnabled(true);

        QBChatService.setConfigurationBuilder(configurationBuilder);
        QBChatService.setDebugEnabled(StringObfuscator.getDebugEnabled());
        QBSessionManager.getInstance().addListener(new QBSessionListenerImpl());
    }

    private void initDb() {
        DataManager.init(this);
    }

    private void initImageLoader(Context context) {
        ImageLoader.getInstance().init(ImageLoaderUtils.getImageLoaderConfiguration(context));
    }

    private void initServices(){
        QMAuthService.init();
        QMUserCache userCache = new QMUserCacheImpl(this);
        QMUserService.init(userCache);
    }

    public synchronized SharedHelper getAppSharedHelper() {
        return appSharedHelper == null
                ? appSharedHelper = new SharedHelper(this)
                : appSharedHelper;
    }

    class QBSessionListener extends QBSessionListenerImpl{

        @Override
        public void onSessionUpdated(QBSessionParameters sessionParameters) {
            QBUser qbUser = AppSession.getSession().getUser();
            qbUser.setPassword(sessionParameters.getUserPassword());
        }
    }
}