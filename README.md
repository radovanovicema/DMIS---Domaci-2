# File Indexer - Višenitno indeksiranje datoteka

Program za višenitno indeksiranje datoteka koji rekurzivno obilazi direktorijume i gradi thread-safe indeks sa filtriranjem, deduplikacijom i periodičnom statistikom.

## Opis arhitekture

Sistem koristi **Producer-Consumer pattern** za paralelno indeksiranje datoteka. 

### Komponente: 

- **FileCrawler (Producer)** - jedna nit rekurzivno obilazi direktorijume i ubacuje validne datoteke u red
- **Indexer (Consumer)** - više niti (broj = `availableProcessors()`) preuzima datoteke iz reda i indeksira ih
- **FileMeta** - model koji čuva podatke o datoteci (putanja, veličina, vreme izmene, ekstenzija)
- **Stats** - thread-safe statistika sa `LongAdder` brojačima
- **SkipReason** - enum sa razlozima preskakanja (HIDDEN, EXT_NOT_ALLOWED, TOO_LARGE, IO_ERROR, NOT_A_FILE, DUPLICATE)
- **Main** - glavna klasa koja pokreće i kontroliše indeksiranje

### Thread-safe strukture: 

- `LinkedBlockingQueue<File>` (kapacitet 100) - ograničeni red za prenos datoteka
- `ConcurrentHashMap<String, FileMeta>` - indeks datoteka
- `ConcurrentHashMap.newKeySet()` - set za dedupliciranje
- `LongAdder` - performantni atomic brojači za statistiku
- `EnumMap` - optimizovana mapa za brojanje po razlozima preskakanja

### Filteri: 

Indeksiraju se samo datoteke sa ekstenzijama `.txt`, `.java`, `.md` koje nisu sakrivene i manje su od 5 MB.

### Dedupliciranje
**Strategija**: Apsolutna putanja

Koristi se File.getAbsolutePath() za identifikaciju datoteka. Svaka apsolutna putanja se čuva u thread-safe Set<String>, gde se atomično proverava i dodaje pomoću visited. add(absPath).

Ako putanja već postoji u setu, add() vraća false i datoteka se preskače kao DUPLICATE.

---

## Gašenje niti

Sistem koristi **Poison Pill + CountDownLatch** mehanizam za korektno gašenje. 

### Tok: 

1. **Producer završava** - Nakon što rekurzivno obiđe sve direktorijume, signalizira završetak kroz `CountDownLatch producersDone`

2. **Slanje poison pill-a** - Main nit čeka producer-a, zatim ubacuje poseban poison pill objekat u red za svakog consumer-a (ukupno `N_CONSUMERS` puta)

3. **Consumer detektuje** - Svaki consumer u petlji poziva `queue.take()`, i kada dobije poison pill objekat (proverava se referencom `==`), izlazi iz petlje i završava

4. **Finalizacija** - Main nit čeka sve consumer-e kroz `CountDownLatch consumersDone`, zatim gasi scheduler i ispisuje rezultate

---

## Periodična statistika

`ScheduledExecutorService` na svakih 1000ms ispisuje trenutno stanje sistema: 

- `found` - ukupno pronađenih datoteka
- `indexed` - broj indeksiranih datoteka
- `queue` - trenutna veličina reda
- `skipped` - broj preskočenih datoteka po razlozima

Scheduler se gasi pozivom `shutdown()` i `awaitTermination()` nakon završetka svih consumer-a. 

---

## Primer izlaza programa
Consumer-1 indeksirao 8 fajlova
[STAT] found=0, indexed=0, queue=0, skipped={HIDDEN=0, EXT_NOT_ALLOWED=0, TOO_LARGE=0, IO_ERROR=0, NOT_A_FILE=0, DUPLICATE=0}
Consumer-2 indeksirao 0 fajlova
Consumer-4 indeksirao 0 fajlova
Consumer-3 indeksirao 0 fajlova
Završeno indeksiranje. Ukupno fajlova u indeksu: 8
FINAL STATS: found=20, indexed=8, skipped={HIDDEN=0, EXT_NOT_ALLOWED=12, TOO_LARGE=0, IO_ERROR=0, NOT_A_FILE=0, DUPLICATE=0}
Primer unosa iz indeksa:
FileMeta{path='C:\Users\Admin\eclipse-workspace\FileIndexer\.\src\fileindexer\Indexer.java', size=2111, lastModified=1768094796979, ext='.java'}
FileMeta{path='C:\Users\Admin\eclipse-workspace\FileIndexer\.\src\fileindexer\FileCrawler.java', size=2829, lastModified=1768134136074, ext='.java'}
FileMeta{path='C:\Users\Admin\eclipse-workspace\FileIndexer\.\src\fileindexer\FileMeta.java', size=1045, lastModified=1768094797003, ext='.java'}
