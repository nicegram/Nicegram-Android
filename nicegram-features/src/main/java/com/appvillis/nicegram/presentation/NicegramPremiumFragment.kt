package com.appvillis.nicegram.presentation

import android.animation.Animator
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.fragment.app.viewModels
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails
import com.appvillis.lib_android_base.BaseFragment
import com.appvillis.lib_android_base.Intents
import com.appvillis.lib_android_base.viewBinding
import com.appvillis.nicegram.NicegramConsts
import com.appvillis.nicegram.R
import com.appvillis.nicegram.databinding.FragmentNicegramPremiumBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NicegramPremiumFragment : BaseFragment(R.layout.fragment_nicegram_premium) {
    companion object {
        const val TAG = "NicegramPremiumDialogFragment"
    }

    private val binding by viewBinding<FragmentNicegramPremiumBinding>()
    private val viewModel by viewModels<NicegramPremiumViewModel>()

    @Inject
    lateinit var billingClient: BillingClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initClickListeners()
        initObservers()

        binding.subscribeButton.animate()
            .alpha(1.0f)
            .setStartDelay(1000)
            .setDuration(500)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (getView() == null) return
                    binding?.subscribeButton?.isEnabled = true
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
    }

    private fun initObservers() {
        viewModel.stateLiveData.observe(viewLifecycleOwner) {

            it.sub?.let { sub ->
                binding.subscribeButton.text =
                    Html.fromHtml(getString(R.string.NicegramSubPerMonth, sub.price))

                binding.subscribeButton.setOnClickListener {
                    launchBillingFlow(sub.sku)
                }
            }
        }

        viewModel.eventCloseScreen.observe(viewLifecycleOwner) {
            requireActivity().onBackPressed()
        }
    }

    private fun launchBillingFlow(skuDetails: SkuDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()

        billingClient.launchBillingFlow(requireActivity(), flowParams)

    }

    private fun initClickListeners() {
        binding.closeBtn.setOnClickListener { requireActivity().onBackPressed() }

        binding.eulaBtn.setOnClickListener {
            Intents.openUrl(
                requireActivity(),
                NicegramConsts.EULA_URL
            )
        }
        binding.privacyBtn.setOnClickListener {
            Intents.openUrl(
                requireActivity(),
                NicegramConsts.PRIVACY_POLICY_URL
            )
        }
    }
}