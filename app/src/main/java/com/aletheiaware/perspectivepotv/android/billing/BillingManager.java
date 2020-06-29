/*
 * Copyright 2020 Aletheia Ware LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aletheiaware.perspectivepotv.android.billing;

import android.app.Activity;
import androidx.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.aletheiaware.perspectivepotv.android.BuildConfig;
import com.aletheiaware.perspectivepotv.android.R;
import com.aletheiaware.perspective.utils.PerspectiveUtils;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchaseState;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BillingManager implements PurchasesUpdatedListener {

    public interface Callback {
        // Called with Billing Client is setup and ready to use.
        void onBillingClientSetup();
        // Called when map of purchases has been updated.
        void onPurchasesUpdated();
        // Called when a purchase has been consumed
        void onTokenConsumed(String purchaseToken);
    }

    private Map<String, Purchase> purchases = new HashMap<>();
    private final Set<String> consumedTokens = new HashSet<>();
    private final Activity activity;
    private final Callback callback;
    private BillingClient client;
    private boolean isConnected = false;

    public BillingManager(Activity activity, final Callback callback) {
        this.activity = activity;
        this.callback = callback;
        Log.d(PerspectiveUtils.TAG, "Creating billing manager.");
        client = BillingClient.newBuilder(activity)
                .enablePendingPurchases()
                .setListener(this)
                .build();
        startServiceConnection(new Runnable() {
            @Override
            public void run() {
                callback.onBillingClientSetup();
                queryPurchases();
            }
        });
    }

    public void destroy() {
        Log.d(PerspectiveUtils.TAG, "Destroying billing manager.");
        if (client != null && client.isReady()) {
            client.endConnection();
            client = null;
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        int code = billingResult.getResponseCode();
        switch (code) {
            case BillingResponseCode.OK:
                if (purchases != null) {
                    for (Purchase purchase : purchases) {
                        handlePurchase(purchase);
                    }
                }
                callback.onPurchasesUpdated();
                break;
            case BillingResponseCode.USER_CANCELED:
                Log.i(PerspectiveUtils.TAG, "User cancelled purchase: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.BILLING_UNAVAILABLE:
                Log.i(PerspectiveUtils.TAG, "Billing unavailable: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.DEVELOPER_ERROR:
                Log.i(PerspectiveUtils.TAG, "Billing developer error: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.FEATURE_NOT_SUPPORTED:
                Log.i(PerspectiveUtils.TAG, "Feature not supported: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.ITEM_ALREADY_OWNED:
                Log.i(PerspectiveUtils.TAG, "Item already owned: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.ITEM_NOT_OWNED:
                Log.i(PerspectiveUtils.TAG, "Item not owned: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.ITEM_UNAVAILABLE:
                Log.i(PerspectiveUtils.TAG, "Item unavailable: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.SERVICE_DISCONNECTED:
                Log.i(PerspectiveUtils.TAG, "Service disconnected: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.SERVICE_TIMEOUT:
                Log.i(PerspectiveUtils.TAG, "Service timeout: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.SERVICE_UNAVAILABLE:
                Log.i(PerspectiveUtils.TAG, "Service unavailable: " + billingResult.getDebugMessage());
                break;
            case BillingResponseCode.ERROR:
                Log.i(PerspectiveUtils.TAG, "Billing error: " + billingResult.getDebugMessage());
                break;
            default:
                Log.i(PerspectiveUtils.TAG, "Unknown result code: " + code + " message: " + billingResult.getDebugMessage());
        }
    }

    public void initiatePurchaseFlow(final SkuDetails skuDetails, final String oldSku) {
        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                Log.d(PerspectiveUtils.TAG, "Launching in-app purchase flow for: " + skuDetails);
                BillingFlowParams.Builder purchaseParamsBuilder = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails);
                if (oldSku != null && !oldSku.isEmpty()) {
                    Log.d(PerspectiveUtils.TAG, "Replacing old SKU: " + oldSku);
                    purchaseParamsBuilder.setOldSku(oldSku);
                }
                client.launchBillingFlow(activity, purchaseParamsBuilder.build());
            }
        });
    }


    public void querySkuDetailsAsync(final String itemType, final List<String> skuList, final SkuDetailsResponseListener listener) {
        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                SkuDetailsParams params = SkuDetailsParams.newBuilder()
                        .setSkusList(skuList)
                        .setType(itemType)
                        .build();
                client.querySkuDetailsAsync(params, listener);
            }
        });
    }

    public void consumeAsync(String purchaseToken, String developerPayload) {
        if (consumedTokens.contains(purchaseToken)) {
            Log.i(PerspectiveUtils.TAG, "Token was already scheduled to be consumed");
            return;
        }
        consumedTokens.add(purchaseToken);

        final ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .setDeveloperPayload(developerPayload)
                .build();
        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                client.consumeAsync(consumeParams, new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                        int code = billingResult.getResponseCode();
                        Log.d(PerspectiveUtils.TAG, "Consume Token Response Code: " + code);
                        if (code == BillingResponseCode.OK) {
                            callback.onTokenConsumed(purchaseToken);
                        }
                    }
                });
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        try {
            byte[] decodedKey = Base64.decode(activity.getString(R.string.app_public_key), Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey key = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
            byte[] signatureBytes = Base64.decode(purchase.getSignature(), Base64.DEFAULT);
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(key);
            sig.update(purchase.getOriginalJson().getBytes());
            if (sig.verify(signatureBytes)) {
                Log.d(PerspectiveUtils.TAG, "Purchase Verified: " + purchase);
                purchases.put(purchase.getSku(), purchase);
                if (!purchase.isAcknowledged()) {
                    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build(), new AcknowledgePurchaseResponseListener() {
                        @Override
                        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                            int code = billingResult.getResponseCode();
                            Log.d(PerspectiveUtils.TAG, "Purchase Acknowledgement Response Code: " + code);
                            if (code == BillingResponseCode.OK) {
                                // TODO
                            }
                        }
                    });
                }
            }
        } catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException | SignatureException e) {
            Log.e(PerspectiveUtils.TAG, "Invalid purchase: " + e);
            e.printStackTrace();
        }
    }

    public Purchase getPurchase(String sku) {
        return purchases.get(sku);
    }

    public boolean hasPurchased(String sku) {
        Purchase purchase = getPurchase(sku);
        return BuildConfig.DEBUG || (purchase != null && purchase.getPurchaseState() == PurchaseState.PURCHASED);
    }

    private void onQueryPurchasesFinished(PurchasesResult purchasesResult) {
        if (client == null) {
            return;
        }

        int code = purchasesResult.getResponseCode();
        Log.d(PerspectiveUtils.TAG, "Purchase Query Response Code: " + code);
        if (code == BillingResponseCode.OK) {
            purchases.clear();
            onPurchasesUpdated(purchasesResult.getBillingResult(), purchasesResult.getPurchasesList());
        }
    }

    public void queryPurchases() {
        executeServiceRequest(new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                PurchasesResult purchasesResult = client.queryPurchases(SkuType.INAPP);
                Log.i(PerspectiveUtils.TAG, "Querying purchases elapsed time: " + (System.currentTimeMillis() - time)
                        + "ms");
                onQueryPurchasesFinished(purchasesResult);
            }
        });
    }

    public void startServiceConnection(final Runnable executeOnSuccess) {
        client.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    int code = billingResult.getResponseCode();
                    Log.d(PerspectiveUtils.TAG, "Setup Response Code: " + code);
                    if (code == BillingClient.BillingResponseCode.OK) {
                        isConnected = true;
                        if (executeOnSuccess != null) {
                            executeOnSuccess.run();
                        }
                    }
                }

            @Override
            public void onBillingServiceDisconnected() {
                isConnected = false;
            }
        });
    }

    private void executeServiceRequest(Runnable runnable) {
        if (isConnected) {
            runnable.run();
        } else {
            startServiceConnection(runnable);
        }
    }
}
