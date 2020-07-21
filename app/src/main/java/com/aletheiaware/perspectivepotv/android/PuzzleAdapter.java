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
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.aletheiaware.perspective.PerspectiveProto.World;
import com.aletheiaware.perspective.utils.PerspectiveUtils;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

public class PuzzleAdapter extends Adapter<PuzzleAdapter.PuzzleViewHolder> {

    public interface Callback {
        void onSelect(String world, int puzzle);
    }

    private final Activity activity;
    private final World world;
    private final Callback callback;
    private final int[] stars;

    public PuzzleAdapter(final Activity activity, World world, int[] stars, Callback callback) {
        this.activity = activity;
        this.world = world;
        this.stars = (stars == null) ? new int[world.getPuzzleCount()] : stars;
        this.callback = callback;
    }

    @NonNull
    @Override
    public PuzzleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final CardView view = (CardView) activity.getLayoutInflater().inflate(R.layout.puzzle_list_item, parent, false);
        final PuzzleViewHolder holder = new PuzzleViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int puzzle = holder.getPuzzle();
                if (!holder.isLocked() && puzzle > 0) {
                    callback.onSelect(world.getName(), puzzle);
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull PuzzleViewHolder holder, int position) {
        int count = world.getPuzzleCount();
        if (count <= 0) {
            holder.setEmptyView();
        } else {
            boolean locked = true;
            if (position == 0) {// First puzzle always unlocked
                locked = false;
            } else if (stars[position] >= 0) {// Puzzle is unlocked if already solved
                locked = false;
            } else if (stars[position - 1] >= 0) {// Puzzle is unlocked if previous is solved
                locked = false;
            }
            holder.set(position + 1, stars[position], locked);
        }
    }

    @Override
    public int getItemCount() {
        int count = world.getPuzzleCount();
        if (count <= 0) {
            return 1;// For empty view
        }
        return count;
    }

    static class PuzzleViewHolder extends RecyclerView.ViewHolder {

        private final Context context;
        private final CardView itemCard;
        private final TextView itemName;
        private final View[] itemStars = new View[PerspectiveUtils.MAX_STARS];
        private final ImageView itemLock;
        private int puzzle;
        private boolean locked;

        PuzzleViewHolder(CardView view) {
            super(view);
            context = view.getContext();
            itemCard = view;
            itemName = view.findViewById(R.id.puzzle_list_text);
            itemStars[0] = view.findViewById(R.id.puzzle_list_star1);
            itemStars[1] = view.findViewById(R.id.puzzle_list_star2);
            itemStars[2] = view.findViewById(R.id.puzzle_list_star3);
            itemStars[3] = view.findViewById(R.id.puzzle_list_star4);
            itemStars[4] = view.findViewById(R.id.puzzle_list_star5);
            itemLock = view.findViewById(R.id.puzzle_list_locked);
        }

        void set(int puzzle, int stars, boolean locked) {
            this.puzzle = puzzle;
            this.locked = locked;
            itemCard.setCardBackgroundColor(ContextCompat.getColor(context, locked ? R.color.grey : R.color.white));
            itemName.setText(String.valueOf(puzzle));
            for (int i = 0; i < PerspectiveUtils.MAX_STARS; i++) {
                itemStars[i].setVisibility(!locked && stars > i ? View.VISIBLE : View.INVISIBLE);
            }
            itemLock.setVisibility(locked ? View.VISIBLE : View.INVISIBLE);
        }

        int getPuzzle() {
            return puzzle;
        }

        boolean isLocked() {
            return locked;
        }

        void setEmptyView() {
            puzzle = -1;
            itemCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.grey));
            itemName.setText(R.string.empty_puzzle_list);
            for (View itemStar : itemStars) {
                itemStar.setVisibility(View.INVISIBLE);
            }
            itemLock.setVisibility(View.INVISIBLE);
        }
    }
}
