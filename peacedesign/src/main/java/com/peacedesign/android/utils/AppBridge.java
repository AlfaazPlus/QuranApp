/*
 * (c) Faisal Khan. Created on 2/9/2021.
 */

package com.peacedesign.android.utils;

import static com.peacedesign.android.utils.AppBridge.Platform.FACEBOOK;
import static com.peacedesign.android.utils.AppBridge.Platform.INSTAGRAM;
import static com.peacedesign.android.utils.AppBridge.Platform.TWITTER;
import static com.peacedesign.android.utils.AppBridge.Platform.WHATSAPP;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.List;

public final class AppBridge {
    private static final String PACKAGE_NAME_WHATSAPP = "com.whatsapp";
    private static final String PACKAGE_NAME_FACEBOOK = "com.facebook.katana";
    private static final String PACKAGE_NAME_TWITTER = "com.twitter.android";
    private static final String PACKAGE_NAME_INSTAGRAM = "com.instagram.android";

    private AppBridge() {
    }

    public static Opener newOpener(Context context) {
        return new Opener(context);
    }

    public static Sharer newSharer(Context context) {
        return new Sharer(context);
    }

    public static String preparePlayStoreLink(Context ctx, boolean toMarket) {
        String packageName = ctx.getPackageName().replace(".debug", "");
        if (toMarket) {
            return "market://details?id=" + packageName;
        } else {
            return "https://play.google.com/store/apps/details?id=" + packageName;
        }
    }

    static String resolvePackage(int platform) {
        switch (platform) {
            case WHATSAPP:
                return PACKAGE_NAME_WHATSAPP;
            case FACEBOOK:
                return PACKAGE_NAME_FACEBOOK;
            case TWITTER:
                return PACKAGE_NAME_TWITTER;
            case INSTAGRAM:
                return PACKAGE_NAME_INSTAGRAM;
        }
        return null;
    }

    public @interface MimeType {
        int TEXT = 1, IMAGE = 2;
    }

    public @interface Platform {
        int SYSTEM_SHARE = 0, WHATSAPP = 1, FACEBOOK = 2, TWITTER = 3, INSTAGRAM = 4;
    }

    public static final class Opener {
        private final Context mContext;
        private final Intent mIntent;

        private Opener(@NonNull Context context) {
            mContext = context;
            mIntent = new Intent();
            mIntent.setAction(Intent.ACTION_VIEW);
        }

        public void openFacebookPage(@NonNull String profileId, @NonNull String fallbackUsername) {
            String uriStr;
            try {
                mContext.getPackageManager().getPackageInfo(resolvePackage(FACEBOOK), 0);
                uriStr = "fb://page/" + profileId;
            } catch (Exception e) {
                e.printStackTrace();
                uriStr = "http://www.facebook.com/" + fallbackUsername;
            }
            mIntent.setData(Uri.parse(uriStr));
            tryOpen(mIntent);
        }

        public void openTwitterProfile(@NonNull String username) {
            String uriStr;
            try {
                mContext.getPackageManager().getPackageInfo(resolvePackage(Platform.TWITTER), 0);
                uriStr = "twitter://user?screen_name=" + username;
            } catch (Exception e) {
                uriStr = "https://twitter.com/" + username;
            }
            mIntent.setData(Uri.parse(uriStr));
            tryOpen(mIntent);
        }

        public void openInstagramProfile(@NonNull String username) {
            String uriStr;

            try {
                mContext.getPackageManager().getPackageInfo(resolvePackage(INSTAGRAM), 0);
                uriStr = "http://instagram.com/_u/" + username;
            } catch (Exception ignored) {
                uriStr = "https://instagram.com/" + username;
            }

            mIntent.setData(Uri.parse(uriStr));
            tryOpen(mIntent);
        }

        public void openPlayStore() {
            try {
                mIntent.setData(Uri.parse(preparePlayStoreLink(mContext, true)));
                mContext.startActivity(mIntent);
            } catch (ActivityNotFoundException ignored) {
                mIntent.setData(Uri.parse(preparePlayStoreLink(mContext, false)));
                mContext.startActivity(mIntent);
            }
        }

        public void browseLink(@NonNull String link) {
            mIntent.setData(Uri.parse(link));
            tryOpen(mIntent);
        }

        private void tryOpen(Intent intent) {
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(mContext, "Failed to open.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static final class Sharer {
        private final Context mContext;
        private final Intent mIntent;
        private int mMimeType;
        private int mPlatform = Platform.SYSTEM_SHARE;
        private String mPackageName;
        private Uri mImageData;
        private CharSequence mTextData;
        private CharSequence mChooserTitle;

        public Sharer(@NonNull Context activity) {
            this.mContext = activity;
            mIntent = new Intent();
            mIntent.setAction(Intent.ACTION_SEND);
        }

        public void share() {
            Intent finalIntent = prepare();
            tryOpen(finalIntent);
        }

        public Intent prepare() {
            //noinspection StatementWithEmptyBody
            if (mTextData != null && mImageData != null) {
                // share both data.
            } else if (mTextData != null) {
                prepareText();
            } else if (mImageData != null) {
                prepareImage();
            }

            mIntent.setTypeAndNormalize(resolveMimeType());

            Intent finalIntent = null;
            if (mPlatform != Platform.SYSTEM_SHARE) {
                if (mPackageName == null) {
                    mPackageName = resolvePackage(mPlatform);
                }

                if (mPackageName != null) {
                    mIntent.setPackage(mPackageName);
                    finalIntent = mIntent;
                }
            }

            if (finalIntent == null) {
                finalIntent = Intent.createChooser(mIntent, mChooserTitle);
            }

            return finalIntent;
        }

        private void tryOpen(Intent finalIntent) {
            try {
                mContext.startActivity(finalIntent);
            } catch (ActivityNotFoundException ignored) {
                sendErrorMessage(mPlatform);
            }
        }

        private void prepareText() {
            mIntent.putExtra(Intent.EXTRA_TEXT, mTextData);
        }

        private void prepareImage() {
            mIntent.putExtra(Intent.EXTRA_STREAM, mImageData);
            mIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        private void sendErrorMessage(int platform) {
            String message = platform + " is not installed on your phone.";
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }

        private String resolveMimeType() {
            switch (mMimeType) {
                case MimeType.TEXT:
                    return "text/plain";
                case MimeType.IMAGE:
                    return "image/*";
            }
            return null;
        }

        private void setMimeType(int intentType) {
            mMimeType = intentType;
        }

        @NonNull
        public Sharer setPlatform(int platform) {
            mPlatform = platform;
            return this;
        }

        @NonNull
        public Sharer setPackage(@NonNull String packageName) {
            mPackageName = packageName;
            return this;
        }

        @NonNull
        public Sharer setData(@NonNull CharSequence textData) {
            mTextData = textData;
            mImageData = null;
            setMimeType(MimeType.TEXT);
            return this;
        }

        @NonNull
        public Sharer setData(@NonNull Uri imageData) {
            mImageData = imageData;
            mTextData = null;
            setMimeType(MimeType.IMAGE);
            return this;
        }

        @NonNull
        public Sharer setChooserTitle(CharSequence title) {
            mChooserTitle = title;
            return this;
        }
    }

    public static final class Email {
        private final Context mContext;
        private final Intent mIntent;
        private CharSequence mChooserTitle;
        private CharSequence mSubject;
        private CharSequence mBody;
        private String[] mRecipients;
        private ResolveInfo mGmailPackage;

        public Email(@NonNull Context activity) {
            this.mContext = activity;
            mIntent = new Intent();
            mIntent.setAction(Intent.ACTION_SEND);
        }

        public void send() {
            Intent finalIntent = prepare();
            tryOpen(finalIntent);
        }

        public Intent prepare() {
            mIntent.setType("text/plain");
            mIntent.putExtra(Intent.EXTRA_EMAIL, mRecipients);
            mIntent.putExtra(Intent.EXTRA_SUBJECT, mSubject);
            mIntent.putExtra(Intent.EXTRA_TEXT, mBody);

            mIntent.setType("message/rfc822");

            final PackageManager pm = mContext.getPackageManager();
            final List<ResolveInfo> matches = pm.queryIntentActivities(mIntent, 0);
            mGmailPackage = null;
            for (final ResolveInfo info : matches) {
                if (info.activityInfo.packageName.endsWith(".gm") || info.activityInfo.name.toLowerCase().contains("gmail")) {
                    mGmailPackage = info;
                }
            }

            return mIntent;
        }

        private void tryOpen(Intent finalIntent) {
            try {
                if (mGmailPackage != null) {
                    mIntent.setClassName(mGmailPackage.activityInfo.packageName, mGmailPackage.activityInfo.name);
                } else {
                    mContext.startActivity(Intent.createChooser(finalIntent, mChooserTitle));
                }
                mContext.startActivity(finalIntent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(mContext, "No email clients installed.", Toast.LENGTH_SHORT).show();
            }
        }

        @NonNull
        public Email setSubject(CharSequence subject) {
            mSubject = subject;
            return this;
        }

        @NonNull
        public Email setBody(CharSequence body) {
            mBody = body;
            return this;
        }

        @NonNull
        public Email setRecipients(String[] recipients) {
            mRecipients = recipients;
            return this;
        }

        @NonNull
        public Email setChooserTitle(CharSequence title) {
            mChooserTitle = title;
            return this;
        }
    }
}