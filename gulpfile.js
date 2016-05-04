var zip = require("zip-local");
var gulp = require("gulp");
var watch = require("gulp-watch") ;

gulp.task("watch" , function() {

    return gulp.src("./Pong/**/*")
            .pipe(watch("./Pong/**/*" , function(){
              zip.sync.zip("./Pong/").compress().save("Pong.zip");
            })) ;

})

gulp.task('default', ['watch']);
