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

package com.aletheiaware.perspectivepotv.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aletheiaware.common.android.utils.CommonAndroidUtils;
import com.aletheiaware.common.utils.CommonUtils;
import com.aletheiaware.perspective.PerspectiveProto.World;
import com.aletheiaware.perspective.PerspectiveProto.Solution;
import com.aletheiaware.perspective.PerspectiveProto.Puzzle;
import com.aletheiaware.perspective.utils.PerspectiveUtils;
import com.aletheiaware.perspectivepotv.android.BuildConfig;
import com.aletheiaware.perspectivepotv.android.PuzzleAdapter;
import com.aletheiaware.perspectivepotv.android.R;
import com.aletheiaware.perspectivepotv.android.WorldAdapter;
import com.aletheiaware.perspectivepotv.android.billing.BillingManager;
import com.aletheiaware.perspectivepotv.android.utils.PerspectiveAndroidUtils;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class WorldSelectActivity extends AppCompatActivity implements WorldAdapter.Callback, BillingManager.Callback {

    private final Map<String, int[]> puzzleStars = new HashMap<>();
    private final Map<String, SkuDetails> skuDetails = new HashMap<>();
    public AlertDialog puzzleListDialog;
    public RecyclerView recyclerView;
    public WorldAdapter adapter;
    private BillingManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_world_select);

        // RecyclerView
        recyclerView = findViewById(R.id.world_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));

        // Adapter
        adapter = new WorldAdapter(this, this);
        recyclerView.setAdapter(adapter);

        manager = new BillingManager(this, this);

        new Thread() {
            @Override
            public void run() {
                for (String world : PerspectiveUtils.FREE_WORLDS) {
                    addWorld(world);
                }
                if (BuildConfig.DEBUG) {
                    for (String world : PerspectiveUtils.PAID_WORLDS) {
                        addWorld(world);
                    }
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (manager != null) {
            manager.destroy();
        }
    }

    @WorkerThread
    private void addWorld(final String world) {
        try {
            final World w = PerspectiveAndroidUtils.getWorld(getAssets(), world);
            final int puzzles = w.getPuzzleCount();
            final int[] stars = new int[puzzles];
            int totalStars = 0;
            for (int i = 0; i < puzzles; i++) {
                stars[i] = -1;
                Solution s = null;
                Puzzle p = w.getPuzzle(i);
                if (p != null) {
                    try {
                        String hash = PerspectiveUtils.getHash(p.toByteArray());
                        s = PerspectiveAndroidUtils.loadSolution(WorldSelectActivity.this, world, hash);
                    } catch (IOException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
                if (s != null) {
                    stars[i] = PerspectiveUtils.scoreToStars(s.getScore(), p.getTarget());
                    totalStars += stars[i];
                }
            }
            puzzleStars.put(world, stars);
            adapter.addWorld(w, totalStars);
        } catch (IOException e) {
            CommonAndroidUtils.showErrorDialog(this, R.style.ErrorDialogTheme, R.string.error_add_world, e);
            e.printStackTrace();
        }
    }

    @Override
    public void onSelect(World world) {
        if (world != null) {
            String name = world.getName();
            View layout = getLayoutInflater().inflate(R.layout.dialog_puzzle_select, null);
            TextView title = layout.findViewById(R.id.puzzle_list_title);
            title.setText(CommonUtils.capitalize(name));
            RecyclerView recycler = layout.findViewById(R.id.puzzle_list_recycler);
            recycler.setLayoutManager(new GridLayoutManager(recycler.getContext(), 3, GridLayoutManager.VERTICAL, false));
            puzzleListDialog = new AlertDialog.Builder(WorldSelectActivity.this, R.style.WorldSelectDialogTheme)
                    .setView(layout)
                    .create();
            PuzzleAdapter puzzleAdapter = new PuzzleAdapter(this, world, puzzleStars.get(name), new PuzzleAdapter.Callback() {
                @Override
                public void onSelect(String world, int puzzle) {
                    puzzleListDialog.cancel();
                    setResult(RESULT_OK);
                    finish();
                    Intent intent = new Intent(WorldSelectActivity.this, GameActivity.class);
                    intent.putExtra(PerspectiveAndroidUtils.WORLD_EXTRA, world);
                    intent.putExtra(PerspectiveAndroidUtils.PUZZLE_EXTRA, puzzle);
                    startActivity(intent);
                }
            });
            recycler.setAdapter(puzzleAdapter);
            puzzleListDialog.show();
        }
    }

    @Override
    public void onBuy(String world) {
        Log.d(PerspectiveUtils.TAG, "Buying: " + world);
        SkuDetails details = skuDetails.get(world);
        Log.d(PerspectiveUtils.TAG, "SKU: " + details);
        manager.initiatePurchaseFlow(details);
    }

    @Override
    public void onBillingClientSetup() {
        Log.d(PerspectiveUtils.TAG, "Billing Client Setup");
        new Thread() {
            @Override
            public void run() {
                querySkuDetails(Arrays.asList(PerspectiveUtils.PAID_WORLDS));
            }
        }.start();
    }

    @Override
    public void onPurchasesUpdated() {
        Log.d(PerspectiveUtils.TAG, "Purchases Updated");
        new Thread() {
            @Override
            public void run() {
                for (String world : PerspectiveUtils.PAID_WORLDS) {
                    if (manager.hasPurchased(world)) {
                        addWorld(world);
                    }
                }
            }
        }.start();
    }

    @Override
    public void onTokenConsumed(String purchaseToken) {
        Log.d(PerspectiveUtils.TAG, "Token Consumed: " + purchaseToken);
        // TODO
    }

    public void querySkuDetails(List<String> skus) {
        Log.d(PerspectiveUtils.TAG, "Querying SKUs: " + skus);
        manager.querySkuDetailsAsync(SkuType.INAPP, skus, new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                int code = billingResult.getResponseCode();
                Log.d(PerspectiveUtils.TAG, "SKU query finished. Response code: " + code);
                if (code == BillingResponseCode.OK) {
                    for (SkuDetails details : skuDetailsList) {
                        Log.d(PerspectiveUtils.TAG, "SKU: " + details);
                        skuDetails.put(details.getSku(), details);
                        adapter.addWorld(details.getSku(), details.getPrice());
                    }
                }
            }
        });
    }
}
