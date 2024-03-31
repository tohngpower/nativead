package com.yourpackage

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import org.json.JSONObject
import kotlin.random.Random

class Contribute : ComponentActivity() {
    private lateinit var nativeAdView: NativeAdView
    private lateinit var adHeadline: TextView
    private lateinit var adBody: TextView
    private lateinit var adPrice: TextView
    private lateinit var adAdvertise: TextView
    private lateinit var adStore: TextView
    private lateinit var thankView: TextView
    private lateinit var countdownView: TextView
    private lateinit var adImage: ImageView
    private lateinit var adIcon: ImageView
    private lateinit var closeButton: ImageView
    private lateinit var starRatingBar: RatingBar
    private lateinit var adMediaView: MediaView
    private lateinit var refreshButton: ImageButton
    private lateinit var actionButton: Button
    private var countdownHandler = Handler(Looper.getMainLooper())
    private val systemDatabase = SystemDatabase(this)
    private var adShowing = true
    private var count = 15.0f
    private var videoPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApp.setAppTheme(this,systemDatabase)
        setContentView(R.layout.activity_contribute)

        nativeAdView = findViewById(R.id.nativeAdView)
        adHeadline = findViewById(R.id.textView20)
        adBody = findViewById(R.id.textView21)
        adPrice = findViewById(R.id.textView23)
        adStore = findViewById(R.id.store)
        adAdvertise = findViewById(R.id.advertise)
        thankView = findViewById(R.id.thankText)
        adImage = findViewById(R.id.imageView12)
        adIcon = findViewById(R.id.imageView13)
        closeButton = findViewById(R.id.imageView14)
        starRatingBar = findViewById(R.id.ad_stars)
        adMediaView = findViewById(R.id.adMedia)
        refreshButton = findViewById(R.id.imageButton6)
        actionButton = findViewById(R.id.ad_call_to_action)
        countdownView = findViewById(R.id.countdown)

        refreshButton.isEnabled = false
        refreshButton.isVisible = false
        actionButton.isVisible = false
        closeButton.isVisible = false
        closeButton.isEnabled = false
        thankView.isVisible = false
        MobileAds.initialize(this) {}
        refreshAd()

        closeButton.setOnClickListener {
            finish()
        }

        refreshButton.setOnClickListener {
            refreshAd()
            refreshButton.isVisible = false
            refreshButton.isEnabled = false
        }
        countdownHandler.post(countdownRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownHandler.removeCallbacks(countdownRunnable)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(!adShowing) {
            super.onBackPressed()
        }
    }

    private fun refreshAd() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(false)
            .build()
        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()
        val adLoader = AdLoader.Builder(this, MyApp.AD_ID)
            .forNativeAd { ad : NativeAd ->
                // Show the ad.
                nativeAdView.mediaView = adMediaView
                nativeAdView.callToActionView = actionButton
                nativeAdView.headlineView = adHeadline
                nativeAdView.iconView = adIcon
                nativeAdView.bodyView = adBody
                nativeAdView.advertiserView = adAdvertise
                nativeAdView.storeView = adStore
                nativeAdView.priceView = adPrice

                if(isDestroyed || isFinishing || isChangingConfigurations) {
                    ad.destroy()
                    return@forNativeAd
                }
                if(ad.icon == null) {
                    adIcon.isVisible = false
                } else {
                    adIcon.setImageDrawable(ad.icon?.drawable)
                }
                if(ad.headline.isNullOrEmpty()) {
                    adHeadline.isVisible = false
                } else {
                    adHeadline.text = ad.headline
                }
                if(ad.advertiser.isNullOrEmpty()) {
                    adAdvertise.isVisible = false
                } else {
                    adAdvertise.text = ad.advertiser
                }
                if(ad.body.isNullOrEmpty()) {
                    adBody.isVisible = false
                } else {
                    adBody.text = ad.body
                }
                if(ad.starRating == null) {
                    starRatingBar.isVisible = false
                } else {
                    starRatingBar.rating = ad.starRating!!.toFloat()
                }
                val price = "Price: <b>${ad.price}</b>"
                if(ad.price.isNullOrEmpty()) {
                    adPrice.isVisible = false
                } else {
                    adPrice.text = Html.fromHtml(price, Html.FROM_HTML_MODE_LEGACY)
                }
                val store = "Store: <b>${ad.store}</b>"
                if(ad.store.isNullOrEmpty()) {
                    adStore.isVisible = false
                } else {
                    adStore.text = Html.fromHtml(store, Html.FROM_HTML_MODE_LEGACY)
                }
                val mediaContent = ad.mediaContent
                if(mediaContent == null) {
                    adMediaView.isVisible = false
                    if(ad.images.size <= 0) {
                        adImage.isVisible = false
                    } else {
                        adImage.setImageDrawable(ad.images[Random.nextInt(ad.images.size)].drawable)
                    }
                } else {
                    adImage.isVisible = false
                    adMediaView.mediaContent = mediaContent
                    // Check if the media content is a video
                    if (mediaContent.hasVideoContent()) {
                        val vc = mediaContent.videoController
                        // Optional: Add a listener to know when the video playback starts/ends
                        vc.let { videoController ->
                            videoController.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                                override fun onVideoPlay() {
                                    videoPlaying = true
                                    super.onVideoPlay()
                                }
                                override fun onVideoEnd() {
                                    // Publishers should allow native ads to complete video playback before
                                    // refreshing or replacing them with another ad in the same UI location.
                                    videoPlaying = false
                                    onComplete()
                                    super.onVideoEnd()
                                }
                            }
                        }
                        vc.play()
                    }
                }
                if(ad.callToAction.isNullOrEmpty()) {
                    actionButton.isVisible = false
                } else {
                    actionButton.isVisible = true
                    actionButton.text = ad.callToAction
                }
                nativeAdView.setNativeAd(ad)

                val text = "Thank you for your contribution!"
                thankView.text = text
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Handle the failure by logging, altering the UI, and so on.
                    val jsonObject = JSONObject(adError.toString())
                    if (jsonObject.has("Message")) {
                        val errorMessage = jsonObject.optString("Message","")
                        thankView.text = errorMessage
                        if(errorMessage == "No fill.") {
                            adIcon.isVisible = false
                            adAdvertise.isVisible = false
                            actionButton.isVisible = false
                            adMediaView.isVisible = false
                            starRatingBar.isVisible = false
                            val appName = "<b>Wandee My AI Assistant</b>"
                            val price = "Price: <b>Free</b>"
                            val store = "Store: <b>Play Store</b>"
                            adHeadline.text = Html.fromHtml(appName, Html.FROM_HTML_MODE_LEGACY)
                            adPrice.text = Html.fromHtml(price, Html.FROM_HTML_MODE_LEGACY)
                            adStore.text = Html.fromHtml(store, Html.FROM_HTML_MODE_LEGACY)
                            val link = "https://play.google.com/store/apps/details?id=com.personal.myai"
                            val download = "Please rate 5 star or write a good review on Play Store<a href=$link><b>[Click here]</b></a>"
                            adBody.text = Html.fromHtml(download, Html.FROM_HTML_MODE_LEGACY)
                            adBody.movementMethod = LinkMovementMethod.getInstance()
                            val text = "Thank you for your contribution!"
                            thankView.text = text
                            adImage.setImageResource(R.drawable.icon)
                        }
                    }
                    onComplete()
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private val countdownRunnable = object : Runnable {
        override fun run() {
            val text = String.format("%.2f second", count)
            countdownView.text = text
            count-=0.01f
            countdownHandler.postDelayed(this,10)
            if(count <= 0.0f) {
                onComplete()
            }
        }
    }

    private fun onComplete() {
        if(!videoPlaying) {
            adShowing = false
            closeButton.isVisible = true
            closeButton.isEnabled = true
            refreshButton.isEnabled = true
            refreshButton.isVisible = true
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        countdownView.text = ""
        thankView.isVisible = true
        countdownHandler.removeCallbacks(countdownRunnable)
    }
}
