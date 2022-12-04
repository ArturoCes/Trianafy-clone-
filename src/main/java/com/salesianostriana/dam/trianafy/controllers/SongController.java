package com.salesianostriana.dam.trianafy.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import com.salesianostriana.dam.trianafy.Dtos.DtoCreateSong;
import com.salesianostriana.dam.trianafy.Dtos.DtoSongConverter;
import com.salesianostriana.dam.trianafy.Dtos.JsonView.SongViews;
import com.salesianostriana.dam.trianafy.model.Artist;
import com.salesianostriana.dam.trianafy.model.Playlist;
import com.salesianostriana.dam.trianafy.model.Song;
import com.salesianostriana.dam.trianafy.service.ArtistService;
import com.salesianostriana.dam.trianafy.service.PlaylistService;
import com.salesianostriana.dam.trianafy.service.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class SongController {

    private final SongService serv;

    private final DtoSongConverter srDto;

    private final ArtistService artSer;

    private final PlaylistService plySer;

    @Operation(summary = "Obtiene una lista de todas las canciones")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Se ha encontrado la lista de canciones",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DtoCreateSong.class))}),
            @ApiResponse(responseCode = "400",
                    description = "No se ha encontrado ninguna lista con canciones",
                    content = @Content),
    })
    @JsonView(SongViews.GetSongPublic.class)
    @GetMapping("/song/")
    public ResponseEntity<List<Song>> findAll() {
        List<Song> songList = serv.findAll();
        if (songList.isEmpty()) {
            return ResponseEntity
                    .notFound()
                    .build();
        } else {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(songList);
        }
    }

    @Operation(summary = "Obtiene la información de una canción por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Se ha encontrado la canción con ese ID",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Song.class))}),
            @ApiResponse(responseCode = "404",
                    description = "No se ha encontrado una canción con ese ID",
                    content = @Content),
    })
    @GetMapping("/song/{id}")
    public ResponseEntity<Song> findById(@PathVariable Long id) {
        if (serv.findById(id).isEmpty()) {
            return ResponseEntity
                    .notFound()
                    .build();
        } else {
            return ResponseEntity
                    .of(serv.findById(id));
        }
    }
    @Operation(summary = "Crea una nueva canción y la añade a la lista")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Se ha creado correctamente la canción",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Song.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Hay un error en los datos o en la petición",
                    content = @Content),
    })
    @JsonView(SongViews.CreateSongPublic.class)
    @PostMapping("/song/")
    public ResponseEntity<Song> addNew(@RequestBody DtoCreateSong song) {

        Optional<Artist> art = artSer.findById(song.getArtistId());

        if (art.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        } else {
            Song newSong = srDto.createSongDtoToSong(song,art.get());
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(serv.add(newSong));
        }

    }

    @Operation(summary = "Editar la información de una canción seleccionada por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Se ha editado correctamentet la canción",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DtoCreateSong.class))}),
            @ApiResponse(responseCode = "404",
                    description = "No se ha encontrado la canción con el ID buscado",
                    content = @Content),
            @ApiResponse(responseCode = "400",
                    description = "Error BAD REQUEST, la petición esta mal",
                    content = @Content),
    })
    @PutMapping("/song/{id}")
    public ResponseEntity<Song> editSong(@RequestBody DtoCreateSong song, @PathVariable Long id) {
        Optional<Artist> art = artSer.findById(song.getArtistId());

        Optional<Song> sng = serv.findById(id);
        if (sng.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        }
        if (art.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        } else {
            sng.get().setAlbum(song.getAlbum());
            sng.get().setYear(song.getYear());
            sng.get().setTitle(song.getTitle());
            sng.get().setArtist(art.get());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(serv.add(sng.get()));
        }
    }

    @Operation(summary = "Elimina la canción que tenga el ID seleccionado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204",
                    description = "Se ha eliminado correctamente la canción",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Song.class))}),
            @ApiResponse(responseCode = "404",
                    description = "No se encuentra ninguna canción relacionadda con ese Id",
                    content = @Content),
    })
    @DeleteMapping("/song/{id}" )
    public ResponseEntity<Song> delete(@PathVariable Long id) {
        Optional<Song> song = serv.findById(id);
        if (song.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        } else {

            List<Playlist> playlist = plySer.findPlaylistBySong(song.get());
            playlist.forEach(playlist1 -> {
                if(playlist1.getSongs().contains(song.get())){
                    playlist1.deleteSong(song.get());
                    plySer.add(playlist1);
                }
            });

            serv.deleteById(id);
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .build();
        }
    }
}
