import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.model.Token;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException; 

import java.util.Arrays;

public class CordovaPluginDynamicGooglepayStripe extends CordovaPlugin {

    private PaymentsClient paymentsClient;
    private String stripePublishableKey;
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 9001;

    CallbackContext callback;

    private void initGooglePayClient(String stripePublishableKey) {
        int environment;
        if (stripePublishableKey.contains("pk_test")) {
            environment = WalletConstants.ENVIRONMENT_TEST;
        } else if (stripePublishableKey.contains("pk_live")) {
            environment = WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            callback.error("Invalid key");
            return;
        }
        this.stripePublishableKey = stripePublishableKey;
        paymentsClient =
                Wallet.getPaymentsClient(this.cordova.getActivity().getApplicationContext(),
                        new Wallet.WalletOptions.Builder().setEnvironment(environment)
                                .build());
       callback.success();
    }

    private void isReadyToPay() {
        IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        Task<Boolean> task = paymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(
                task1 -> {
                    try {
                        boolean result = task1.getResult(ApiException.class);
                        if(result) {
                            //show Google as payment option
                            callback.success();
                        } else {
                            //hide Google as payment option
                            callback.error("Google Pay not supported");
                        }
                    } catch (ApiException exception) {
                        callback.error(exception.getMessage());
                    }
                });
    }

    private PaymentMethodTokenizationParameters createTokenizationParameters() {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "stripe")
                .addParameter("stripe:publishableKey", stripePublishableKey)
                .addParameter("stripe:version", "6.1.2")
                .build();
    }

    private void requestPayment (String totalPrice, String currency) {
        PaymentDataRequest request = this.createPaymentDataRequest(totalPrice, currency);
        Activity activity = this.cordova.getActivity();
        if (request != null) {
            cordova.setActivityResultCallback(this);
            AutoResolveHelper.resolveTask(
                    paymentsClient.loadPaymentData(request),
                    activity,
                    LOAD_PAYMENT_DATA_REQUEST_CODE);
        }
    }

    private PaymentDataRequest createPaymentDataRequest(String totalPrice, String currency) {
        PaymentDataRequest.Builder request =
                PaymentDataRequest.newBuilder()
                        .setTransactionInfo(
                                TransactionInfo.newBuilder()
                                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                        .setTotalPrice(totalPrice)
                                        .setCurrencyCode(currency)
                                        .build())
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .setCardRequirements(
                                CardRequirements.newBuilder()
                                        .addAllowedCardNetworks(Arrays.asList(
                                                WalletConstants.CARD_NETWORK_AMEX,
                                                WalletConstants.CARD_NETWORK_DISCOVER,
                                                WalletConstants.CARD_NETWORK_VISA,
                                                WalletConstants.CARD_NETWORK_MASTERCARD))
                                        .build());

        request.setPaymentMethodTokenizationParameters(createTokenizationParameters());
        return request.build();
    }

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data) {
       System.out.println("onActivityResult reached correctly");
       switch (requestCode) {
           case LOAD_PAYMENT_DATA_REQUEST_CODE:
               switch (resultCode) {
                   case Activity.RESULT_OK:
                       PaymentData paymentData = PaymentData.getFromIntent(data);
                       // You can get some data on the user's card, such as the brand and last 4 digits
                       CardInfo info = paymentData.getCardInfo();
                       // You can also pull the user address from the PaymentData object.
                       UserAddress address = paymentData.getShippingAddress();
                       // This is the raw JSON string version of your Stripe token.
                       String rawToken = paymentData.getPaymentMethodToken().getToken();

                       // Now that you have a Stripe token object, charge that by using the id
                       Token stripeToken = Token.fromString(rawToken);
                       if (stripeToken != null) {
                           // This chargeToken function is a call to your own server, which should then connect
                           // to Stripe's API to finish the charge.
                           this.callback.success(stripeToken.getId());
                       } else {
                           this.callback.error("An error occurred");
                       }
                       break;
                   case Activity.RESULT_CANCELED:
                       this.callback.error("Payment cancelled");
                       break;
                   case AutoResolveHelper.RESULT_ERROR:
                       Status status = AutoResolveHelper.getStatusFromIntent(data);
                       // Log the status for debugging
                       // Generally there is no need to show an error to
                       // the user as the Google Payment API will do that
                       break;
                   default:
                       // Do nothing.
               }
               break; // Breaks the case LOAD_PAYMENT_DATA_REQUEST_CODE
           // Handle any other startActivityForResult calls you may have made.
           default:
               // Do nothing.
       }
   }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callback = callbackContext; 
        if(action.equals("init_google_pay_client")) {
            initGooglePayClient(args.getString(0));
        }
        if(action.equals("is_ready_to_pay")) {
            isReadyToPay();
        } else if(action.equals("request_payment")) {
            requestPayment(args.getString(0), args.getString(1));
        } else {
            return false;
        }
            return true;
    }

}