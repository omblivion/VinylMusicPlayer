package com.poupa.vinylmusicplayer.ui.activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.adapter.SearchAdapter;
import com.poupa.vinylmusicplayer.databinding.ActivitySearchBinding;
import com.poupa.vinylmusicplayer.discog.Discography;
import com.poupa.vinylmusicplayer.search.SearchController;
import com.poupa.vinylmusicplayer.search.SearchRepository;
import com.poupa.vinylmusicplayer.model.Album;
import com.poupa.vinylmusicplayer.model.Artist;
import com.poupa.vinylmusicplayer.model.Song;
import com.poupa.vinylmusicplayer.ui.activities.base.AbsMusicServiceActivity;
import com.poupa.vinylmusicplayer.util.ImageTheme.ThemeStyleUtil;
import com.poupa.vinylmusicplayer.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SearchActivity extends AbsMusicServiceActivity implements SearchView.OnQueryTextListener, SearchController.Listener {

    public static final String QUERY = "query";

    RecyclerView recyclerView;
    Toolbar toolbar;
    TextView empty;
    ProgressBar progress;

    SearchView searchView;

    SearchAdapter adapter;
    private String query;
    private SearchController controller;
    private SearchState searchState;
    private final Runnable libraryChanged = () -> {
        if (controller != null && hasPermissions()) controller.refresh();
    };

    public static class SearchState extends ViewModel {
        final SearchRepository repository = new SearchRepository(Discography.getInstance());
        @Override protected void onCleared() {
            repository.clear();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySearchBinding binding = ActivitySearchBinding.inflate(LayoutInflater.from(this));
        recyclerView = binding.recyclerView;
        toolbar = binding.toolbar;
        empty = binding.empty;
        progress = binding.searchProgress;
        setContentView(binding.getRoot());

        setDrawUnderStatusbar();

        setStatusbarColorAuto();
        setNavigationbarColorAuto();
        setTaskDescriptionColorAuto();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ThemeStyleUtil.getInstance().setHeaderPadding(recyclerView, getResources().getDisplayMetrics().density);

        //change scrollbar color to follow secondary color
        Drawable unwrappedDrawable = AppCompatResources.getDrawable(this, R.drawable.scrollbar_vertical_thumb);
        Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
        DrawableCompat.setTint(wrappedDrawable, ThemeStore.accentColor(this));

        adapter = new SearchAdapter(this, Collections.emptyList());
        recyclerView.setAdapter(adapter);

        recyclerView.setOnTouchListener((v, event) -> {
            hideSoftKeyboard();
            return false;
        });

        setUpToolBar();

        if (savedInstanceState != null) {
            query = savedInstanceState.getString(QUERY);
        }

        searchState = new ViewModelProvider(this).get(SearchState.class);
    }

    @Override protected void onStart() {
        super.onStart();
        controller = new SearchController(searchState.repository, new Handler(Looper.getMainLooper()),
                new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(), action -> new Thread(action, "NeedleSearch")), this);
        Discography.getInstance().addChangedListener(libraryChanged);
    }

    @Override protected void onResume() {
        super.onResume();
        updateSearch();
    }

    @Override protected void onStop() {
        Discography.getInstance().removeChangedListener(libraryChanged);
        if (controller != null) {
            controller.close();
            controller = null;
        }
        super.onStop();
    }

    @Override protected void onDestroy() {
        if (controller != null) controller.close();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(QUERY, query);
    }

    private void setUpToolBar() {
        toolbar.setBackgroundColor(ThemeStore.primaryColor(this));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        final MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchItem.expandActionView();
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                onBackPressed();
                return false;
            }
        });

        searchView.setQuery(query, false);
        searchView.setOnQueryTextListener(this);
        updateSearch();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void search(@NonNull String query) {
        this.query = query;
        updateSearch();
    }

    private void updateSearch() {
        if (controller == null) return;
        if (hasPermissions()) {
            controller.submit(query);
        } else {
            controller.submit("");
            searchState.repository.clear();
            empty.setVisibility(View.GONE);
        }
    }

    @Override protected void onHasPermissionsChanged(boolean hasPermissions) {
        super.onHasPermissionsChanged(hasPermissions);
        updateSearch();
    }

    @Override
    public void onMediaStoreChanged() {
        super.onMediaStoreChanged();
        if (controller != null && hasPermissions()) controller.refresh();
    }

    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();

        // give the adapter a chance to update the decoration
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPlayStateChanged() {
        super.onPlayStateChanged();

        // give the adapter a chance to update the decoration
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        search(query);
        hideSoftKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        search(newText);
        return true;
    }

    private void hideSoftKeyboard() {
        Util.hideSoftKeyboard(SearchActivity.this);
        if (searchView != null) {
            searchView.clearFocus();
        }
    }

    @Override
    public void onPending() {
        adapter.swapDataSet(Collections.emptyList());
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResults(List<Object> data) {
        List<Object> rows = new ArrayList<>();
        int previousCategory = -1;
        for (Object item : data) {
            int category = item instanceof Song ? R.string.songs
                    : item instanceof Artist ? R.string.artists : R.string.albums;
            if (category != previousCategory) {
                rows.add(getString(category));
                previousCategory = category;
            }
            rows.add(item);
        }
        adapter.swapDataSet(rows);
        progress.setVisibility(View.GONE);
        empty.setText(R.string.no_results);
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onError() {
        adapter.swapDataSet(Collections.emptyList());
        progress.setVisibility(View.GONE);
        empty.setText(R.string.search_error);
        empty.setVisibility(View.VISIBLE);
    }
}
