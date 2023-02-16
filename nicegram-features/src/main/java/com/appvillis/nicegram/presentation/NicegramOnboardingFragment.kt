package com.appvillis.nicegram.presentation

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.appvillis.lib_android_base.BaseFragment
import com.appvillis.lib_android_base.viewBinding
import com.appvillis.nicegram.R
import com.appvillis.nicegram.databinding.FragmentNgOnboardingBinding
import com.appvillis.nicegram.databinding.ItemNgOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NicegramOnboardingFragment : BaseFragment(R.layout.fragment_ng_onboarding) {

    private val binding by viewBinding<FragmentNgOnboardingBinding>()

    private val viewModel by viewModels<NicegramOnboardingViewModel>()

    private val adapter by lazy {
        OnboardingAdapter(
            LayoutInflater.from(requireContext()), listOf(
                OnboardingVH.OnboardingItem(
                    R.string.NicegramOnbroadingTitle1,
                    R.string.NicegramOnbroadingText1,
                    R.raw.ng_onboarding_1
                ),
                OnboardingVH.OnboardingItem(
                    R.string.NicegramOnbroadingTitle2,
                    R.string.NicegramOnbroadingText2,
                    R.raw.ng_onboarding_2
                ),
                OnboardingVH.OnboardingItem(
                    R.string.NicegramOnbroadingTitle3,
                    R.string.NicegramOnbroadingText3,
                    R.raw.ng_onboarding_3
                ),
                OnboardingVH.OnboardingItem(
                    R.string.NicegramOnbroadingTitle4,
                    R.string.NicegramOnbroadingText4,
                    R.raw.ng_onboarding_4
                ),
                OnboardingVH.OnboardingItem(
                    R.string.NicegramOnbroadingTitle5,
                    R.string.NicegramOnbroadingText5,
                    R.raw.ng_onboarding_5
                ),
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViewPager()
        initClickListeners()

        viewModel.onViewCreated()
    }

    private fun initClickListeners() {
        binding.continueButton.setOnClickListener {
            if (binding.viewPager.currentItem == adapter.itemCount - 1) {
                binding.continueButton.isEnabled = false
                binding.continueButton.isClickable = false
                requireActivity().onBackPressed()
            } else binding.viewPager.currentItem++
        }
    }

    private fun initViewPager() {
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 10
        binding.viewPager.isUserInputEnabled = false

        binding.indicatorView.attachTo(binding.viewPager)
        binding.indicatorView.isInvisible = true
        binding.indicatorView.postDelayed( { binding.indicatorView.isInvisible = false }, 1000)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                adapter.holders[position]?.startVideo()
                if (position == adapter.itemCount - 1) {
                    binding.continueButton.text = getString(R.string.NicegramGetStarted)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        adapter.holders.forEach {
            if (binding.viewPager.currentItem == it.key) it.value.startVideo()
        }
    }

    class OnboardingAdapter(
        private val inflater: LayoutInflater,
        private val items: List<OnboardingVH.OnboardingItem>
    ) : RecyclerView.Adapter<OnboardingVH>() {
        val holders = mutableMapOf<Int, OnboardingVH>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            OnboardingVH(ItemNgOnboardingBinding.inflate(inflater, parent, false))

        override fun onBindViewHolder(holder: OnboardingVH, position: Int) {
            holders[position] = holder
            holder.bind(items[position], position != 0)
            holder.itemView.setOnClickListener { holder.binding.videoView.start() }
        }

        override fun getItemCount() = items.size
    }

    class OnboardingVH(val binding: ItemNgOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var videoViewIsPrepared = false
        var seekTo = 0

        fun startVideo() {
            binding.videoView.post {
                if (videoViewIsPrepared) {
                    binding.videoView.start()
                    if (seekTo > 0) binding.videoView.seekTo(seekTo)
                }
            }
        }

        fun pauseVideo() {
            binding.videoView.post {
                binding.videoView.pause()
            }
        }

        fun bind(item: OnboardingItem, needSeek: Boolean) {
            binding.title.setText(item.title)
            binding.desc.setText(item.desc)

            val path = "android.resource://${itemView.context.packageName}/${item.video}"

            if (binding.videoView.tag != path) {
                binding.videoView.tag = path
                binding.videoView.setOnPreparedListener {
                    seekTo = if (needSeek) 500 else 0
                    videoViewIsPrepared = true
                }
                videoViewIsPrepared = false
                binding.videoView.postDelayed({ binding.videoView.setVideoURI(Uri.parse(path)) }, 0)
            }
        }

        class OnboardingItem(
            @StringRes val title: Int,
            @StringRes val desc: Int,
            @RawRes val video: Int
        )
    }
}