package com.drafael.springboot.reactor.app;

import com.drafael.springboot.reactor.app.models.Comentarios;
import com.drafael.springboot.reactor.app.models.Usuario;
import com.drafael.springboot.reactor.app.models.UsuarioComentarios;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner{

	private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(SpringBootReactorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		ejemploContraPresion();
		
	}
	public void ejemploContraPresion(){

		Flux.range(1,10)
				.log()
				//.limitRate(5)
				.subscribe(new Subscriber<Integer>() {
					private Subscription s;
					private Integer limite = 5 ;
					private Integer consumido = 0 ;

					@Override
					public void onSubscribe(Subscription s) {

						this.s = s;
						s.request(limite);
					}

					@Override
					public void onNext(Integer integer) {

						log.info(integer.toString());
						consumido++;
						if (consumido==limite){
							consumido=0;
							s.request(limite);
						}
					}

					@Override
					public void onError(Throwable t) {

					}

					@Override
					public void onComplete() {

					}
				});
	}

	public void ejemploIntervalDesdeCreate(){
		Flux.create(emitter -> {
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				private Integer contador = 0;
				@Override
				public void run() {
					emitter.next(++contador);
					if (contador==10){
						timer.cancel();
						emitter.complete();
					}
				}
			}, 1000, 100);
		})
		.doOnNext(next -> log.info(next.toString()))
		.doOnComplete(()-> log.info("Hemos terminado"))
		.subscribe();
	}

	public void ejemploIntervalInfinito() throws InterruptedException {

		CountDownLatch latch = new CountDownLatch(1);

		Flux.interval(Duration.ofSeconds(1))
		.doOnTerminate(latch::countDown)
		.flatMap(i -> {
			if( i>= 5){
				return Flux.error(new InterruptedException("Solo, hasta 5!"));
			}
			return Flux.just(i);
		})
		.map(i -> "Hola"+i)
		.retry(2)
		.doOnNext(s-> log.info(s))
		.subscribe();
		latch.await();
	}

	public void ejemploDelayElements(){
		Flux<Integer> rango = Flux.range(1,12)
				.delayElements(Duration.ofSeconds(1))
				.doOnNext(i-> log.info(i.toString()));
		//rango.subscribe();
		rango.blockLast();
	}

	public void ejemploInterval(){
		Flux<Integer> rango = Flux.range(1,12);
		Flux<Long> retraso = Flux.interval(Duration.ofSeconds(1));
		rango.zipWith(retraso, (ra,re) -> ra)
				.doOnNext(i -> log.info(i.toString()))
				.blockLast();
				//.subscribe();
	}

	public void ejemploUsuarioComentariosZipWitRangos(){
		Flux<Integer> rangos = Flux.range(0,4);
		Flux.just(1,2,3,4)
				.map(i -> (i*2))
				.zipWith(rangos, (uno, dos) -> String.format("Primer flux %d, Segundo flux: %d", uno,dos))
				//.zipWith(Flux.range(0,4), (uno, dos) -> String.format("Primer flux %d, Segundo flux: %d", uno,dos))
				.subscribe(texto -> log.info(texto));
	}

	public void ejemploUsuarioComentariosZipWithForma2(){
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));

		Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable( ()->{
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola");
			comentarios.addComentario("Sideral");
			comentarios.addComentario("Y la");
			comentarios.addComentario("Chocolatada?");
			return comentarios;
		});
		Mono<UsuarioComentarios> usuarioConComentariosMono = usuarioMono
				.zipWith(comentariosUsuarioMono)
				.map(tuple -> {
					Usuario u  = tuple.getT1();
					Comentarios c = tuple.getT2();
					return new UsuarioComentarios(u,c);
				});
		usuarioConComentariosMono.subscribe(uc -> log.info(uc.toString()));
	}

	public void ejemploUsuarioComentariosZipWith(){
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));

		Mono<Comentarios> comentariosMono = Mono.fromCallable( ()->{
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola");
			comentarios.addComentario("Como");
			comentarios.addComentario("Van");
			comentarios.addComentario("Las entradas?");
			return comentarios;
		});
		Mono<UsuarioComentarios> usuarioConComentariosMono = usuarioMono.zipWith(comentariosMono, (usuario, comentariosUsuario) -> new UsuarioComentarios(usuario, comentariosUsuario));
		usuarioConComentariosMono.subscribe(uc -> log.info(uc.toString()));
	}

	public void ejemploUsuarioComentariosFlatMap(){
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));

		Mono<Comentarios> comentariosMono = Mono.fromCallable( ()->{
			Comentarios comentarios = new Comentarios();
			comentarios.addComentario("Hola");
			comentarios.addComentario("Sideral");
			comentarios.addComentario("Y la");
			comentarios.addComentario("Chocolatada?");
			return comentarios;
		});
		usuarioMono.flatMap(u -> comentariosMono.map(c -> new UsuarioComentarios(u, c)))
		.subscribe(uc -> log.info(uc.toString()));
	}


    public void ejemploCollectList() throws Exception {

        //Flatmap convierte en Mono o FLux
        List<Usuario> usu = new ArrayList<>();
        usu.add(new Usuario("Diego","Flores"));
        usu.add(new Usuario("Rafael", "Sabina"));
        usu.add(new Usuario("Mia", "Nia"));
        usu.add(new Usuario("Rodri","Pool"));
        usu.add(new Usuario("Joaco", "Tzu"));
        usu.add(new Usuario("Daniela", "Tan"));
        usu.add(new Usuario("Dani", "Chang"));

        Flux.fromIterable(usu)
                .collectList()
                .subscribe(lista -> {
                    lista.forEach(item ->  log.info(lista.toString()));
                });

    }

	public void ejemploToString() throws Exception {

		//Flatmap convierte en Mono o FLux
		List<Usuario> usu = new ArrayList<>();
		usu.add(new Usuario("Diego","Flores"));
		usu.add(new Usuario("Rafael", "Sabina"));
		usu.add(new Usuario("Mia", "Nia"));
		usu.add(new Usuario("Rodri","Pool"));
		usu.add(new Usuario("Joaco", "Tzu"));
		usu.add(new Usuario("Daniela", "Tan"));
		usu.add(new Usuario("Dani", "Chang"));

		Flux.fromIterable(usu)
				.map(usuario -> usuario.getNombre().toUpperCase() +" "+ usuario.getApellido().toUpperCase())
				.flatMap(nombre -> {
					if(nombre.contains("diego".toUpperCase())){
						return Mono.just(nombre);
					} else{
						return Mono.empty();
					}
				})
				.map(nombre -> {
					return nombre.toLowerCase();
				})
				.subscribe(u-> log.info(u.toString()));

	}

	public void ejemploFlatMap() throws Exception {

		//Flatmap convierte en Mono o FLux
		List<String> usu = new ArrayList<>();
		usu.add("Diego Flores\"");
		usu.add("Rafael Sabina");
		usu.add("Mia Nia");
		usu.add("Rodri Pool\"");
		usu.add("Joaco Tzu");
		usu.add("Daniela Tan");
		usu.add("Dani Chang");

		Flux.fromIterable(usu)
				.map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(),
						nombre.split(" ")[1].toUpperCase()))
				.flatMap(usuario -> {
					if(usuario.getNombre().equalsIgnoreCase("diego")){
						return Mono.just(usuario);
					} else{
						return Mono.empty();
					}
				})
				.map(usuario -> {
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					return usuario;
				})
				.subscribe(u-> log.info(u.toString()));

	}


	public void ejemploIterable() throws Exception {

		List<String> usu = new ArrayList<>();
		usu.add("Diego Flores\"");
		usu.add("Rafael Sabina");
		usu.add("Mia Nia");
		usu.add("Rodri Pool\"");
		usu.add("Joaco Tzu");
		usu.add("Daniela Tan");
		usu.add("Dani Chang");

		Flux<String> nombres = Flux.fromIterable(usu);
		//Flux<String> nombres = Flux.just("Diego Flores","Rafael Sabina","Mia Nia","Rodri Pool","Joaco Tzu", "Daniela Tan", "Dani Chang");

		Flux<Usuario> usuarios =nombres.map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(),
				nombre.split(" ")[1].toUpperCase()))
				.filter(usuario -> usuario.getNombre().equalsIgnoreCase("diego"))
				.doOnNext(e-> {
					if(e == null) {
						throw new RuntimeException("Fail nombre");
					}
					System.out.println(e.getNombre() + " & "+ e.getApellido());
				})
				.map(usuario -> {
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					return usuario;
				});
		//.doOnNext(nombre -> System.out.println(nombre));
				/*.doOnNext(nombre -> {
					System.out.println(nombre);

				});*/
		//nombres.subscribe(log::info);
		usuarios.subscribe(e-> log.info(e.toString()),
				error -> log.error(error.getMessage()),
				new Runnable() {
					@Override
					public void run() {
						log.info("Finalizo la app");
					}
				});

	}

}
