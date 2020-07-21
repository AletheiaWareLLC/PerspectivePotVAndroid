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

package com.aletheiaware.perspectivepotv.android;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.aletheiaware.common.utils.CommonUtils;
import com.aletheiaware.perspective.PerspectiveProto.World;
import com.aletheiaware.perspective.utils.PerspectiveUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public class WorldAdapter extends Adapter<ViewHolder> {

    public interface Callback {
        void onSelect(World world);
        void onBuy(String world);
    }

    private final Activity activity;
    private final List<String> names = new ArrayList<>();
    private final Map<String, Integer> starsMap = new HashMap<>();
    private final Map<String, String> pricesMap = new HashMap<>();
    private final Map<String, World> worldsMap = new HashMap<>();
    private final Callback callback;
    private int visible = 1;

    public WorldAdapter(Activity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public synchronized void addWorld(World world, boolean completed, int stars) {
        String name = world.getName();
        System.out.println("Adding World: " + name + " " + completed + " " + stars);
        if (!names.contains(name)) {
            names.add(name);
            if (completed) {
                visible++;
            }
        }
        worldsMap.put(name, world);
        starsMap.put(name, stars);
        sort();
    }

    public synchronized void addWorld(String name, String price) {
        if (!names.contains(name)) {
            names.add(name);
        }
        pricesMap.put(name, price);
        sort();
    }

    private synchronized void sort() {
        // Sort names of free worlds first, then paid worlds second
        Collections.sort(names, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int i1 = -1;
                int i2 = -1;
                for (int i = 0; i < PerspectiveUtils.FREE_WORLDS.length; i++) {
                    String w = PerspectiveUtils.FREE_WORLDS[i];
                    if (o1.equals(w)) {
                        i1 = i;
                    }
                    if (o2.equals(w)) {
                        i2 = i;
                    }
                }
                for (int i = 0; i < PerspectiveUtils.PAID_WORLDS.length; i++) {
                    String w = PerspectiveUtils.PAID_WORLDS[i];
                    if (o1.equals(w)) {
                        i1 = i + PerspectiveUtils.FREE_WORLDS.length;
                    }
                    if (o2.equals(w)) {
                        i2 = i + PerspectiveUtils.FREE_WORLDS.length;
                    }
                }
                return Integer.compare(i1, i2);
            }
        });
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = activity.getLayoutInflater().inflate(R.layout.world_list_item, parent, false);
        final WorldViewHolder holder = new WorldViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onSelect(holder.getWorld());
            }
        });
        Button buyButton = view.findViewById(R.id.world_item_buy);
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onBuy(holder.getName());
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorldViewHolder wvh = (WorldViewHolder) holder;
        if (names.isEmpty()) {
            wvh.setEmptyView();
        } else {
            String name = names.get(position);
            World world = worldsMap.get(name);
            if (world == null) {
                String price = pricesMap.get(name);
                if (price == null) {
                    price = "?";
                }
                wvh.set(name, price);
            } else {
                int s = 0;
                Integer stars = starsMap.get(name);
                if (stars != null) {
                    s = stars;
                }
                int puzzles = world.getPuzzleCount();
                wvh.set(world, s, puzzles*PerspectiveUtils.MAX_STARS);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        if (names.isEmpty()) {
            return 1;// Empty view
        }
        return Math.min(visible, names.size());
    }

    static class WorldViewHolder extends ViewHolder {

        private final TextView itemName;
        private final TextView itemTitle;
        private final ImageView itemStarImage;
        private final TextView itemStarText;
        private final Button itemBuy;
        private String name;
        private String title;
        private World world;

        WorldViewHolder(View view) {
            super(view);
            itemName = view.findViewById(R.id.world_item_name);
            itemTitle = view.findViewById(R.id.world_item_title);
            itemStarImage = view.findViewById(R.id.world_item_star);
            itemStarText = view.findViewById(R.id.world_item_star_count);
            itemBuy = view.findViewById(R.id.world_item_buy);
        }

        void set(World world, int earnedStars, int maxStars) {
            setWorld(world);
            setName(world.getName());
            setTitle(world.getTitle());
            itemBuy.setVisibility(View.GONE);
            if (earnedStars <= 0) {
                itemStarImage.setVisibility(View.GONE);
                itemStarText.setVisibility(View.GONE);
            } else{
                itemStarImage.setVisibility(View.VISIBLE);
                itemStarText.setVisibility(View.VISIBLE);
                itemStarText.setText(itemStarText.getContext().getString(R.string.star_count_format, earnedStars, maxStars));
            }
        }

        void set(String name, String price) {
            setWorld(null);
            setName(name);
            setTitle(null);
            itemStarImage.setVisibility(View.GONE);
            itemStarText.setVisibility(View.GONE);
            itemBuy.setText(price);
            itemBuy.setVisibility(View.VISIBLE);
        }

        void setName(String name) {
            this.name = name;
            itemName.setText(CommonUtils.capitalize(name));
        }

        void setTitle(String title) {
            this.title = title;
            if (title == null || title.isEmpty()) {
                itemTitle.setVisibility(View.GONE);
            } else {
                itemTitle.setText(CommonUtils.capitalize(title));
                itemTitle.setVisibility(View.VISIBLE);
            }
        }

        void setWorld(World world) {
            this.world = world;
        }

        String getName() {
            return name;
        }

        String getTitle() {
            return title;
        }

        World getWorld() {
            return world;
        }

        void setEmptyView() {
            itemName.setText(R.string.empty_world_list);
            itemTitle.setVisibility(View.GONE);
            itemStarImage.setVisibility(View.GONE);
            itemStarText.setVisibility(View.GONE);
            itemBuy.setVisibility(View.GONE);
        }
    }

}
