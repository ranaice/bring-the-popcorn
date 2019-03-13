package anaice.com.br.bringthepopcorn.ui.moviedetail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import anaice.com.br.bringthepopcorn.R;
import anaice.com.br.bringthepopcorn.data.db.AppDatabase;
import anaice.com.br.bringthepopcorn.data.db.entity.FavoriteMovie;
import anaice.com.br.bringthepopcorn.data.model.Genre;
import anaice.com.br.bringthepopcorn.data.model.Movie;
import anaice.com.br.bringthepopcorn.data.model.MovieReview;
import anaice.com.br.bringthepopcorn.data.model.MovieTrailer;
import anaice.com.br.bringthepopcorn.data.model.MovieTrailers;
import anaice.com.br.bringthepopcorn.data.network.MainService;
import anaice.com.br.bringthepopcorn.ui.main.MainActivity;
import anaice.com.br.bringthepopcorn.util.AppExecutors;
import anaice.com.br.bringthepopcorn.util.DateUtils;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailActivity extends AppCompatActivity implements TrailerAdapter.TrailerClickedListener {

    private MainService mMainService;
    private int mMovieId;
    private ImageView mMovieBigPosterIv;
    private ImageView mMovieSmallPosterIv;
    private ImageView mMovieStarRatingIv;
    private ImageView mMovieBookmarkIv;
    private TextView mMovieRatingTv;
    private TextView mMovieTitleTv;
    private TextView mMovieReleaseDateTv;
    private TextView mMovieOverviewTv;
    private TextView mMovieGenreTv;
    private TextView mMovieLabelTitle;
    private TextView mMovieLabelGenre;
    private TextView mMovieLabelReleaseDate;
    private TextView mMovieLabelOverview;
    private TextView mEmptyText;
    private LinearLayout mMovieReviewsLayout;
    private LinearLayout mMovieTrailersLayout;
    private RecyclerView mMovieReviewsRv;
    private RecyclerView mMovieTrailersRv;
    private AppDatabase mDb;
    private Movie mMovie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        Intent intent = getIntent();
        mMovieId = intent.getIntExtra(MainActivity.MOVIE_ID, 0);

        mMainService = new MainService();
        mDb = AppDatabase.getInstance(getApplicationContext());

        initViews();
        setViewsListeners();
        fetchMovie();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initViews() {

        mMovieBigPosterIv = findViewById(R.id.iv_movie_big_poster);
        mMovieSmallPosterIv = findViewById(R.id.iv_movie_poster);
        mMovieStarRatingIv = findViewById(R.id.iv_movie_star_rating);
        mMovieTitleTv = findViewById(R.id.tv_movie_title);
        mMovieRatingTv = findViewById(R.id.tv_movie_rating);
        mMovieReleaseDateTv = findViewById(R.id.tv_movie_release_date);
        mMovieOverviewTv = findViewById(R.id.tv_movie_overview);
        mMovieGenreTv = findViewById(R.id.tv_movie_genre);
        mMovieLabelTitle = findViewById(R.id.label_movie_title);
        mMovieLabelGenre = findViewById(R.id.label_movie_genre);
        mMovieLabelReleaseDate = findViewById(R.id.label_movie_release_date);
        mMovieLabelOverview = findViewById(R.id.label_movie_overview);
        mEmptyText = findViewById(R.id.tv_empty_text);
        mMovieReviewsLayout = findViewById(R.id.llayout_movie_reviews);
        mMovieReviewsRv = findViewById(R.id.rv_movie_reviews);
        mMovieTrailersLayout = findViewById(R.id.llayout_movie_trailers);
        mMovieTrailersRv = findViewById(R.id.rv_movie_trailers);
        mMovieBookmarkIv = findViewById(R.id.iv_movie_bookmark);

        mEmptyText.setVisibility(View.INVISIBLE);
    }

    private void setViewsListeners() {
        mMovieBookmarkIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if ()
                saveFavoriteMovie();
            }
        });
    }

    private void fetchMovie() {
        mMainService.getMovieService().getMovie(mMovieId).enqueue(new Callback<Movie>() {
            @Override
            public void onResponse(@NonNull Call<Movie> call, @NonNull Response<Movie> response) {
                if (response.isSuccessful()) {
                    mMovie = response.body();
                    if (mMovie != null) {
                        fillScreenMovieData(mMovie);
                        setupToolbar(mMovie.getTitle());
                    }
                } else {
                    resetScreen();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Movie> call, @NonNull Throwable t) {
                t.printStackTrace();
                resetScreen();
            }
        });
    }

    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void fillScreenMovieData(Movie movie) {
        Picasso.get().load(MainService.IMAGE_URL_500 + movie.getBackdrop_path()).into(mMovieBigPosterIv);
        Picasso.get().load(MainService.IMAGE_URL_185 + movie.getPoster_path()).into(mMovieSmallPosterIv);
        mMovieTitleTv.setText(movie.getTitle());
        mMovieRatingTv.setText(String.valueOf(movie.getVote_average()));
        mMovieReleaseDateTv.setText(DateUtils.getBrazilianDateFormat(movie.getRelease_date()));

        StringBuilder sb = new StringBuilder();

        for (Genre genre : movie.getGenres()) {
            sb.append(genre.getName()).append(", ");
        }

        String movieGenre = sb.substring(0, sb.lastIndexOf(","));

        mMovieGenreTv.setText(movieGenre);
        mMovieOverviewTv.setText(movie.getOverview());

        fetchReviews();
        fetchTrailers();
    }

    private void fetchReviews() {
        mMainService.getMovieService().getReviews(mMovieId).enqueue(new Callback<MovieReview>() {
            @Override
            public void onResponse(@NonNull Call<MovieReview> call, @NonNull Response<MovieReview> response) {
                if (response.isSuccessful()) {
                    MovieReview reviews = response.body();
                    if (reviews != null && reviews.getUserReviews() != null && reviews.getUserReviews().size() > 0) {
                        mMovieReviewsLayout.setVisibility(View.VISIBLE);
                        mMovieReviewsRv.setHasFixedSize(true);
                        mMovieReviewsRv.setLayoutManager(new LinearLayoutManager(MovieDetailActivity.this));
                        mMovieReviewsRv.setAdapter(new ReviewAdapter(MovieDetailActivity.this,
                                                                     reviews.getUserReviews()));
                    } else {
                        mMovieReviewsLayout.setVisibility(View.GONE);
                    }
                } else {
                    mMovieReviewsLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieReview> call, @NonNull Throwable t) {
                t.printStackTrace();
                mMovieReviewsLayout.setVisibility(View.GONE);
            }
        });
    }

    private void fetchTrailers() {
        mMainService.getMovieService().getTrailers(mMovieId).enqueue(new Callback<MovieTrailers>() {
            @Override
            public void onResponse(@NonNull Call<MovieTrailers> call, @NonNull Response<MovieTrailers> response) {
                if (response.isSuccessful()) {
                    MovieTrailers trailers = response.body();
                    if (trailers != null && trailers.getTrailers() != null && trailers.getTrailers().size() > 0) {
                        mMovieTrailersLayout.setVisibility(View.VISIBLE);
                        mMovieTrailersRv.setHasFixedSize(true);
                        mMovieTrailersRv.setLayoutManager(new LinearLayoutManager(MovieDetailActivity.this));
                        mMovieTrailersRv.setAdapter(new TrailerAdapter(MovieDetailActivity.this,
                                                                      trailers.getTrailers(),
                                                                      MovieDetailActivity.this));
                    } else {
                        mMovieTrailersLayout.setVisibility(View.GONE);
                    }
                } else {
                    mMovieTrailersLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieTrailers> call,@NonNull Throwable t) {
                mMovieTrailersLayout.setVisibility(View.GONE);
            }
        });
    }

    private void openMovieTrailer(String videoPath) {
        Intent youtubeIntent = new Intent(Intent.ACTION_VIEW);
        youtubeIntent.setData(Uri.parse("https://www.youtube.com/watch?v=" + videoPath));
        if (youtubeIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(youtubeIntent);
        }
    }

    @Override
    public void onTrailerClicked(MovieTrailer trailer) {
        openMovieTrailer(trailer.getKey());
    }

    private void saveFavoriteMovie() {
        if (mMovie != null) {
            Log.d("MovieDetail", "Salvando favorito...");
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    mDb.favoriteMovieDao().insert(new FavoriteMovie(mMovie.getId(), mMovie.getTitle()));
                    Log.d("MovieDetail", "Insert ocorreu...");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("MovieDetail", "Atualizando imagem do favorito");
                            mMovieBookmarkIv.setImageDrawable(getDrawable(R.drawable.ic_bookmark_yellow_24dp));
                        }
                    });
                }
            });
        }
    }

    private void deleteFavoriteMovie() {
        if (mMovie != null) {
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    mDb.favoriteMovieDao().delete(new FavoriteMovie(mMovie.getId(), mMovie.getTitle()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMovieBookmarkIv.setImageDrawable(getDrawable(R.drawable.ic_bookmark_border_black_24dp));
                        }
                    });
                }
            });
        }

    }

    private void resetScreen() {
        mEmptyText.setVisibility(View.VISIBLE);
        mMovieTitleTv.setText("");
        mMovieGenreTv.setText("");
        mMovieReleaseDateTv.setText("");
        mMovieRatingTv.setText("");
        mMovieOverviewTv.setText("");

        mMovieLabelTitle.setText("");
        mMovieLabelOverview.setText("");
        mMovieLabelReleaseDate.setText("");
        mMovieLabelGenre.setText("");

        //Image Views
        mMovieSmallPosterIv.setImageResource(android.R.color.transparent);
        mMovieBigPosterIv.setImageResource(android.R.color.transparent);
        mMovieStarRatingIv.setImageResource(android.R.color.transparent);
    }

}
