package ru.alexbykov.nopaginate.paginate;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import ru.alexbykov.nopaginate.callback.ObserverCallback;
import ru.alexbykov.nopaginate.callback.OnLoadMore;
import ru.alexbykov.nopaginate.callback.OnRepeatListener;
import ru.alexbykov.nopaginate.item.DefaultGridLayoutItem;
import ru.alexbykov.nopaginate.item.ErrorItem;
import ru.alexbykov.nopaginate.item.LoadingItem;
import ru.alexbykov.nopaginate.paginate.grid.WrapperSpanSizeLookup;


/**
 * @author Alex Bykov and Marko Milos, original repository: https://github.com/MarkoMilos/Paginate
 */


public class Paginate implements ObserverCallback, OnRepeatListener {


    private int loadingTriggerThreshold;
    private RecyclerView recyclerView;
    private OnLoadMore paginateCallback;
    private WrapperAdapter wrapperAdapter;
    private LoadingItem loadingItem;
    private ErrorItem errorItem;
    private WrapperAdapterObserver wrapperAdapterObserver;
    private RecyclerView.Adapter userAdapter;
    private WrapperSpanSizeLookup wrapperSpanSizeLookup;
    private boolean isError;
    private boolean isLoading;
    private boolean isLoadedAllItems;


    Paginate(RecyclerView recyclerView, OnLoadMore paginateCallback, int loadingTriggerThreshold, LoadingItem loadingItem, ErrorItem errorItem) {
        this.recyclerView = recyclerView;
        this.loadingTriggerThreshold = loadingTriggerThreshold;
        this.paginateCallback = paginateCallback;
        this.loadingItem = loadingItem;
        this.errorItem = errorItem;
        setupWrapper();
        setupScrollListener();
    }

    private void setupWrapper() {
        this.userAdapter = recyclerView.getAdapter();
        wrapperAdapter = new WrapperAdapter(userAdapter, loadingItem, errorItem);
        wrapperAdapterObserver = new WrapperAdapterObserver(this, wrapperAdapter);
        userAdapter.registerAdapterDataObserver(wrapperAdapterObserver);
        recyclerView.setAdapter(wrapperAdapter);
        wrapperAdapter.setOnRepeatListener(this);
        checkGridLayoutManager();
    }

    private void checkGridLayoutManager() {
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            DefaultGridLayoutItem item = new DefaultGridLayoutItem(recyclerView.getLayoutManager());
            wrapperSpanSizeLookup = new WrapperSpanSizeLookup(((GridLayoutManager) recyclerView.getLayoutManager()).getSpanSizeLookup(),
                    item,
                    wrapperAdapter);
            ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(wrapperSpanSizeLookup);
        }
    }


    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                checkScroll();
            }
        });
    }

    private void checkAdapterState() {
        if (isCanLoadMore()) {
            paginateCallback.onLoadMore();
        }
    }

    private boolean isCanLoadMore() {
        return !isLoading && !isError && !isLoadedAllItems;
    }

    @Override
    public void onAdapterChange() {

        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                final PaginateStatus status = PaginateStatus.getStatus(isLoadedAllItems, isError);
                wrapperAdapter.stateChanged(status);
                checkScroll();
            }
        });
    }


    private void checkScroll() {
        if (ScrollUtils.isOnBottom(recyclerView, loadingTriggerThreshold)) {
            checkAdapterState();
        }
    }

    public void showError(boolean show) {
        if (show) {
            isError = true;
            wrapperAdapter.stateChanged(PaginateStatus.ERROR);
            ScrollUtils.fullScrollToBottom(recyclerView, wrapperAdapter);
        } else {
            isError = false;
        }
    }


    public void showLoading(boolean show) {
        if (show) {
            isLoading = true;
            wrapperAdapter.stateChanged(PaginateStatus.LOADING);
        } else {
            isLoading = false;
        }
    }

    public void setPaginateNoMoreItems(boolean isNoMoreItems) {
        if (isNoMoreItems) {
            this.isLoadedAllItems = true;
            wrapperAdapter.stateChanged(PaginateStatus.NO_MORE_ITEMS);
        } else {
            this.isLoadedAllItems = false;
        }
    }

    @Override
    public void onClickRepeat() {
        showError(false);
        checkScroll();
    }


    public void unSubscribe() {
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            wrapperAdapter.unSubscribe();
            userAdapter.unregisterAdapterDataObserver(wrapperAdapterObserver);
            recyclerView.setAdapter(userAdapter);
        } else if (recyclerView.getLayoutManager() instanceof GridLayoutManager && wrapperSpanSizeLookup != null) {
            GridLayoutManager.SpanSizeLookup spanSizeLookup = wrapperSpanSizeLookup.getWrappedSpanSizeLookup();
            ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(spanSizeLookup);
        }
    }

}

