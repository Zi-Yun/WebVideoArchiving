package blog.cmcmcmcm.webvideoarchiving.fragment

import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import blog.cmcmcmcm.webvideoarchiving.R
import blog.cmcmcmcm.webvideoarchiving.activity.MainActivity
import blog.cmcmcmcm.webvideoarchiving.activity.VideoActivity
import blog.cmcmcmcm.webvideoarchiving.data.Video
import blog.cmcmcmcm.webvideoarchiving.databinding.FragmentArchiveBinding
import blog.cmcmcmcm.webvideoarchiving.fragment.adapter.ArchiveAdapter
import blog.cmcmcmcm.webvideoarchiving.util.RxBus.RxBus
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where

class ArchiveFragment : Fragment() {

    lateinit var binding: FragmentArchiveBinding
    lateinit var realm: Realm

    val disposables: CompositeDisposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_archive, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initToolbar()
    }

    override fun onStart() {
        super.onStart()

        //비디오 액티비티에서 다시 돌아올 때, 정상작동을 위해 여기서 초기화.
        realm = Realm.getDefaultInstance()

        initRecyclerView()
    }

    private fun initToolbar() {
        (activity as MainActivity).setSupportActionBar(binding.toolbar?.toolbarArchive)
    }

    //initialize recycler view
    private fun initRecyclerView() {
        val adapter = ArchiveAdapter(activity).apply {
            updateItems(realm.where<Video>().sort(Video::addedDate.name, Sort.DESCENDING).findAll())

            disposables.add(clickSubject
                    .subscribe { data ->
                        //상세정보 보는 화면으로 이동
                        RxBus.getInstance().takeBus(data)

                        startActivity(Intent(activity, VideoActivity::class.java))
                    })
        }

        val layoutManager = LinearLayoutManager(activity)

        binding.recyclerArchive.apply {
            this.adapter = adapter
            this.layoutManager = layoutManager
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    //첫번째로 보이는 아이템의 포지션 얻기
                    val firstPosition = layoutManager.findFirstVisibleItemPosition()

                        val globalVisibleRect = Rect()
                        val itemVisibleRect = Rect()

                        binding.recyclerArchive.getGlobalVisibleRect(globalVisibleRect)

                        val view = layoutManager.findViewByPosition(firstPosition)

                        if (view != null && view.height > 0 && view.getGlobalVisibleRect(itemVisibleRect)) {
                            val visiblePercent =
                                    if (itemVisibleRect.bottom >= globalVisibleRect.bottom) {
                                        val visibleHeight = globalVisibleRect.bottom - itemVisibleRect.top
                                        Math.min(visibleHeight.toFloat() / view.height, 1f)
                                    } else {
                                        val visibleHeight = itemVisibleRect.bottom - globalVisibleRect.top
                                        Math.min(visibleHeight.toFloat() / view.height, 1f)
                                    }

                            val viewHolder =
                                    binding.recyclerArchive.findViewHolderForAdapterPosition(firstPosition) as ArchiveAdapter.ArchiveViewHolder

                            //90%이상 보이면 재생, 그렇지 않으면 멈춤.
                            if (visiblePercent >= 0.9f) {
                                viewHolder.play()
                            } else  {
                                viewHolder.stop()
                            }
                        }
                    }
            })
        }
    }

    private fun releaseResources() {
        binding.recyclerArchive.adapter = null
        disposables.clear()
        realm.close()
    }


    override fun onStop() {
        super.onStop()

        releaseResources()
    }

    override fun onDestroy() {
        super.onDestroy()

        releaseResources()
    }
}