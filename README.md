# Skeleton Project

Welcome to Skeleton project, which built with MVVM architecture and Jetpack Compose.

## Major Highlights

- **Jetpack Compose** for modern UI
- **Offline caching** with a **single source of truth**
- **MVVM architecture** for a clean and scalable codebase
- **Kotlin** and **Kotlin DSL**
- **Dagger Hilt** for efficient dependency injection.
- **Ktor** for seamless networking
- **Coroutines** and **Flow** for asynchronous programming
- **StateFlow** for streamlined state management
- **Pagination** to efficiently load and display 
- **Unit tests** and **UI tests** for robust code coverage
- **Instant search** for quick access to data
- **Navigation** for smooth transitions between screens
- **WebView** for a seamless reading experience
- **WorkManager** for periodic items fetching
- **Notification** for alerting 
- **Coil** for efficient image loading
- Pull to refresh for refreshing  content
- Swipe to delete 


## Dependency Use

- Jetpack Compose for UI: Modern UI toolkit for building native Android UIs
- Coil for Image Loading: Efficiently loads and caches images
- Ktor for Networking: A type-safe HTTP client for smooth network requests
- Dagger Hilt for Dependency Injection: Simplifies dependency injection
- Paging Compose for Pagination: Simplifies the implementation of paginated lists
- Mockito, JUnit, Turbine for Testing: Ensures the reliability of the application

## How to Run the Project

- Clone the Repository:
```
git clone https://github.com/Sirius-ITS/Oman-MTCIT.git
```
buildConfigField("String", "API_KEY", "\"<Add your API Key>\"")
```
- Replace "Add your API Key" with the API key you obtained
- Build and run the NewsApp.


## The Complete Project Folder Structure

```
|── NewsApplication.kt
├── common
│   ├── Const.kt
│   ├── NoInternetException.kt
│   ├── dispatcher
│   │   ├── DefaultDispatcherProvider.kt
│   │   └── DispatcherProvider.kt
│   ├── logger
│   │   ├── AppLogger.kt
│   │   └── Logger.kt
│   ├── networkhelper
│   │   ├── NetworkHelper.kt
│   │   └── NetworkHelperImpl.kt
│   └── util
│       ├── EntityUtil.kt
│       ├── NavigationUtil.kt
│       ├── TimeUtil.kt
│       ├── Util.kt
│       └── ValidationUtil.kt
├── data
│   ├── database
│   │   ├── AppDatabaseService.kt
│   │   ├── ArticleDatabase.kt
│   │   ├── DatabaseService.kt
│   │   ├── dao
│   │   │   ├── ArticleDao.kt
│   │   │   └── SavedArticleDao.kt
│   │   └── entity
│   │       ├── Article.kt
│   │       ├── SavedArticleEntity.kt
│   │       ├── SavedSourceEntity.kt
│   │       └── Source.kt
│   ├── model
│   │   ├── ApiArticle.kt
│   │   ├── ApiSource.kt
│   │   ├── Country.kt
│   │   ├── Language.kt
│   │   ├── News.kt
│   │   └── Sources.kt
│   ├── network
│   │   ├── ApiInterface.kt
│   │   └── ApiKeyInterceptor.kt
│   └── repository
│       └── NewsRepository.kt
├── di
│   ├── module
│   │   └── ApplicationModule.kt
│   └── qualifiers.kt
├── ui
│   ├── NewsActivity.kt
│   ├── base
│   │   ├── CommonUI.kt
│   │   ├── NewsDestination.kt
│   │   ├── NewsNavigation.kt
│   │   └── UIState.kt
│   ├── components
│   │   ├── Article.kt
│   │   ├── FilterItem.kt
│   │   ├── FilterItemListLayout.kt
│   │   └── NewsListLayout.kt
│   ├── paging
│   │   └── NewsPagingSource.kt
│   ├── screens
│   │   ├── ArticleScreen.kt
│   │   ├── FilterScreen.kt
│   │   ├── NewsScreen.kt
│   │   ├── SavedScreen.kt
│   │   └── SearchScreen.kt
│   ├── theme
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── viewmodels
│       ├── NewsViewModel.kt
│       ├── SearchViewModel.kt
│       ├── SharedViewModel.kt
│       └── filters
│           ├── CountryFilterViewModel.kt
│           ├── LanguageFilterViewModel.kt
│           └── SourceFilterViewModel.kt
└── worker
    └── NewsWorker.kt
```

<p align="center">
<img alt="screenshots"  src="https://github.com/khushpanchal/NewsApp/blob/master/assets/News_app.jpeg">
</p>

## If this project helps you, show love ❤️ by putting a ⭐ on this project ✌️

## Contribute to the project

Feel free to improve or add features to the project.
Create an issue or find the pending issue. All pull requests are welcome 😄

## Other projects
I have also created RVTimeTracker - RecyclerView Time Tracker, a finely crafted library designed to accurately calculate viewing time for each item in RecyclerView.
Check it out - [RVTimeTracker](https://github.com/khushpanchal/RVTimeTracker)

