package com.appvillis.nicegram.presentation

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RawRes
import androidx.annotation.StringRes
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
                OnboardingItem(
                    R.string.NicegramOnbroadingTitle1,
                    R.string.NicegramOnbroadingText1,
                    R.raw.ng_onboarding_1
                ),
                OnboardingItem(
                    R.string.NicegramOnbroadingTitle2,
                    R.string.NicegramOnbroadingText2,
                    R.raw.ng_onboarding_2
                ),
                OnboardingItem(
                    R.string.NicegramOnbroadingTitle3,
                    R.string.NicegramOnbroadingText3,
                    R.raw.ng_onboarding_3
                ),
                OnboardingItem(
                    R.string.NicegramOnbroadingTitle4,
                    R.string.NicegramOnbroadingText4,
                    R.raw.ng_onboarding_4
                ),
                OnboardingItem(
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
            }
            else binding.viewPager.currentItem++
        }
    }

    private fun initViewPager() {
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 10
        binding.viewPager.isUserInputEnabled = false

        binding.indicatorView.attachTo(binding.viewPager)

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
            if (adapter.currentItem == it.key) it.value.startVideo()
        }
    }

    override fun onPause() {
        super.onPause()

        adapter.holders.forEach {
            if (adapter.currentItem == it.key) it.value.startVideo()
        }
    }


    class OnboardingAdapter(
        private val inflater: LayoutInflater,
        private val items: List<OnboardingItem>
    ) : RecyclerView.Adapter<OnboardingVH>() {
        var currentItem = 0
            set(value) {
                field = value
                notifyItemChanged(value)
            }

        val holders = mutableMapOf<Int, OnboardingVH>()


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            OnboardingVH(ItemNgOnboardingBinding.inflate(inflater, parent, false))

        override fun onBindViewHolder(holder: OnboardingVH, position: Int) {
            holders[position] = holder
            holder.bind(items[position], position != 0)
        }

        override fun getItemCount() = items.size
    }

    class OnboardingVH(private val binding: ItemNgOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun startVideo() {
            binding.videoView.post { binding.videoView.start() }
        }

        fun pauseVideo() {
            binding.videoView.post { binding.videoView.pause() }
        }

        fun bind(item: OnboardingItem, needSeek: Boolean) {
            binding.title.setText(item.title)
            binding.desc.setText(item.desc)

            val path = "android.resource://${itemView.context.packageName}/${item.video}"

            if (binding.videoView.tag != path) {
                binding.videoView.tag = path
                binding.videoView.setVideoURI(Uri.parse(path))
                binding.videoView.setOnPreparedListener {
                    if (needSeek) binding.videoView.seekTo(500)
                }
            }
        }
    }

    class OnboardingItem(
        @StringRes val title: Int,
        @StringRes val desc: Int,
        @RawRes val video: Int
    )
}