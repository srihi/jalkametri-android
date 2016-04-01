/**
 * Copyright 2010 Google Inc. All Rights Reserved.
 * 
 * This file is part of jAlkaMetri.
 * 
 * jAlkaMetri is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * jAlkaMetri is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with jAlkaMetri (LICENSE.txt). If not, see <http://www.gnu.org/licenses/>.
 */
package fi.tuska.jalkametri.billing;

import java.security.Security;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import fi.tuska.jalkametri.Common.PurchaseState;
import fi.tuska.jalkametri.Common.ResponseCode;
import fi.tuska.jalkametri.PrivateData;
import fi.tuska.jalkametri.billing.BillingService.RequestPurchase;
import fi.tuska.jalkametri.billing.BillingService.RestoreTransactions;
import fi.tuska.jalkametri.dao.Preferences;
import fi.tuska.jalkametri.data.PreferencesImpl;
import fi.tuska.jalkametri.util.BillingUtil;
import fi.tuska.jalkametri.util.LogUtil;

/**
 * This class contains the methods that handle responses from Android Market.
 * The implementation of these methods is specific to a particular
 * application. The methods in this example update the database and, if the
 * main application has registered a {@llink PurchaseObserver}, will also
 * update the UI. An application might also want to forward some responses on
 * to its own server, and that could be done here (in a background thread) but
 * this example does not do that.
 * 
 * You should modify and obfuscate this code before using it.
 */
public class ResponseHandler {

    private static final String TAG = "ResponseHandler";

    /**
     * This is a static instance of {@link PurchaseObserver} that the
     * application creates and registers with this class. The PurchaseObserver
     * is used for updating the UI if the UI is visible.
     */
    private static PurchaseObserver sPurchaseObserver;

    /**
     * Registers an observer that updates the UI.
     * 
     * @param observer the observer to register
     */
    public static synchronized void register(PurchaseObserver observer) {
        sPurchaseObserver = observer;
    }

    /**
     * Unregisters a previously registered observer.
     * 
     * @param observer the previously registered observer.
     */
    public static synchronized void unregister(PurchaseObserver observer) {
        sPurchaseObserver = null;
    }

    /**
     * Notifies the application of the availability of the
     * MarketBillingService. This method is called in response to the
     * application calling {@link BillingService#checkBillingSupported()}.
     * 
     * @param supported true if in-app billing is supported.
     */
    public static void checkBillingSupportedResponse(boolean supported) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.onBillingSupported(supported);
        }
    }

    /**
     * Starts a new activity for the user to buy an item for sale. This method
     * forwards the intent on to the PurchaseObserver (if it exists) because
     * we need to start the activity on the activity stack of the application.
     * 
     * @param pendingIntent a PendingIntent that we received from Android
     * Market that will create the new buy page activity
     * @param intent an intent containing a request id in an extra field that
     * will be passed to the buy page activity when it is created
     */
    public static void buyPageIntentResponse(PendingIntent pendingIntent, Intent intent) {
        if (sPurchaseObserver == null) {
            LogUtil.d(TAG, "UI is not running");
            return;
        }
        sPurchaseObserver.startBuyPageActivity(pendingIntent, intent);
    }

    /**
     * Notifies the application of purchase state changes. The application can
     * offer an item for sale to the user via
     * {@link BillingService#requestPurchase(String)}. The BillingService
     * calls this method after it gets the response. Another way this method
     * can be called is if the user bought something on another device running
     * this same app. Then Android Market notifies the other devices that the
     * user has purchased an item, in which case the BillingService will also
     * call this method. Finally, this method can be called if the item was
     * refunded.
     * 
     * @param purchaseState the state of the purchase request (PURCHASED,
     * CANCELED, or REFUNDED)
     * @param productId a string identifying a product for sale
     * @param orderId a string identifying the order
     * @param purchaseTime the time the product was purchased, in milliseconds
     * since the epoch (Jan 1, 1970)
     * @param developerPayload the developer provided "payload" associated
     * with the order
     */
    public static void purchaseResponse(final Context context, final PurchaseState purchaseState,
        final String productId, final String orderId, final long purchaseTime,
        final String developerPayload) {

        LogUtil.i(TAG, "Purchase response for %s: %s", productId, purchaseState);

        if (PrivateData.LICENSE_PRODUCT_ID.equals(productId)) {
            // Update the database with the purchase state.
            Preferences prefs = new PreferencesImpl(context);
            final boolean hasLicense = purchaseState == PurchaseState.PURCHASED;
            if (hasLicense) {
                BillingUtil.setLicensePurchased(prefs);
            } else {
                BillingUtil.removeLicenseInfo(prefs);
            }

            if (sPurchaseObserver != null) {
                sPurchaseObserver.postLicenseStateChanged(hasLicense);
            }

            /*
             * // We don't update the UI here. We will update the UI after we
             * // update new Thread(new Runnable() { public void run() { //
             * Notify the main program of this synchronized
             * (ResponseHandler.class) { if (sPurchaseObserver != null) {
             * sPurchaseObserver.postLicenseStateChanged(hasLicense); } } }
             * }).start();
             */
        } else {
            LogUtil.w(TAG, "Unknown product: %s", productId);
        }
    }

    /**
     * This is called when we receive a response code from Android Market for
     * a RequestPurchase request that we made. This is used for reporting
     * various errors and also for acknowledging that an order was sent
     * successfully to the server. This is NOT used for any purchase state
     * changes. All purchase state changes are received in the
     * {@link BillingReceiver} and are handled in
     * {@link Security#verifyPurchase(String, String)}.
     * 
     * @param context the context
     * @param request the RequestPurchase request for which we received a
     * response code
     * @param responseCode a response code from Market to indicate the state
     * of the request
     */
    public static void responseCodeReceived(Context context, RequestPurchase request,
        ResponseCode responseCode) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.onRequestPurchaseResponse(request, responseCode);
        }
    }

    /**
     * This is called when we receive a response code from Android Market for
     * a RestoreTransactions request.
     * 
     * @param context the context
     * @param request the RestoreTransactions request for which we received a
     * response code
     * @param responseCode a response code from Market to indicate the state
     * of the request
     */
    public static void responseCodeReceived(Context context, RestoreTransactions request,
        ResponseCode responseCode) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.onRestoreTransactionsResponse(request, responseCode);
        }
    }
}
