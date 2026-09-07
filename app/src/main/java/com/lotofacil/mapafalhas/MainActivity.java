package com.lotofacil.mapafalhas;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 1001;
    private static final int PURPLE = Color.rgb(106, 27, 154);
    private static final int PURPLE_DARK = Color.rgb(74, 16, 110);
    private static final int GREEN = Color.rgb(20, 107, 50);
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private final List<Concurso> concursos = new ArrayList<>();
    private TextView txtArquivo, txtStatus, txtMapa, txtFalhas, txtJogo, txtPadrao, txtRanking;
    private EditText edtInicio, edtFim;

    static class Concurso {
        final int numero;
        final Set<Integer> dezenas;
        Concurso(int numero, Set<Integer> dezenas) {
            this.numero = numero;
            this.dezenas = dezenas;
        }
    }

    static class Score {
        final int dezena;
        final double valor;
        final double f5;
        final double f10;
        final int seq;
        Score(int dezena, double valor, double f5, double f10, int seq) {
            this.dezena = dezena;
            this.valor = valor;
            this.f5 = f5;
            this.f10 = f10;
            this.seq = seq;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(criarTela());
    }

    private View criarTela() {
        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        page.setBackgroundColor(Color.rgb(245, 242, 248));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(24));
        page.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(14), dp(14), dp(14));
        header.setBackgroundColor(PURPLE);
        root.addView(header, lp(-1, -2, 0, 0, 0, 12));

        ImageView icon = new ImageView(this);
        icon.setImageResource(com.lotofacil.mapafalhas.R.drawable.ic_lotofacil);
        header.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1f);
        header.addView(titles, titleLp);

        TextView t1 = label("LOTOFÁCIL • MAPA DE FALHAS", 18, Color.WHITE, true);
        TextView t2 = label("Estudo do intervalo e projeção das 10 falhas", 13, Color.WHITE, false);
        titles.addView(t1);
        titles.addView(t2);

        Button btnArquivo = new Button(this);
        btnArquivo.setText("1. ESCOLHER ARQUIVO DE RESULTADOS");
        btnArquivo.setOnClickListener(v -> escolherArquivo());
        root.addView(btnArquivo, lp(-1, -2, 0, 0, 0, 4));

        txtArquivo = label("Nenhum arquivo selecionado", 13, Color.DKGRAY, false);
        root.addView(txtArquivo, lp(-1, -2, 0, 0, 0, 10));

        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(linha, lp(-1, -2, 0, 0, 0, 8));

        edtInicio = new EditText(this);
        edtInicio.setHint("Concurso inicial");
        edtInicio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        linha.addView(edtInicio, new LinearLayout.LayoutParams(0, -2, 1f));

        edtFim = new EditText(this);
        edtFim.setHint("Concurso final");
        edtFim.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams endLp = new LinearLayout.LayoutParams(0, -2, 1f);
        endLp.setMargins(dp(8), 0, 0, 0);
        linha.addView(edtFim, endLp);

        Button btnAnalisar = new Button(this);
        btnAnalisar.setText("2. GERAR MAPA E ANALISAR TENDÊNCIA");
        btnAnalisar.setOnClickListener(v -> analisar());
        root.addView(btnAnalisar, lp(-1, -2, 0, 0, 0, 8));

        txtStatus = label("Aguardando arquivo...", 14, Color.BLACK, true);
        root.addView(txtStatus, lp(-1, -2, 0, 0, 0, 14));

        root.addView(section("MAPA DO INTERVALO"));
        HorizontalScrollView h = new HorizontalScrollView(this);
        ScrollView v = new ScrollView(this);
        txtMapa = label("O mapa aparecerá aqui.", 11, Color.BLACK, false);
        txtMapa.setTypeface(Typeface.MONOSPACE);
        txtMapa.setPadding(dp(8), dp(8), dp(8), dp(8));
        v.addView(txtMapa, new ScrollView.LayoutParams(-2, -2));
        h.addView(v, new HorizontalScrollView.LayoutParams(-2, dp(330)));
        root.addView(h, lp(-1, dp(330), 0, 4, 0, 14));

        root.addView(section("PALPITE DAS 10 FALHAS"));
        txtFalhas = box("--", 20, PURPLE_DARK);
        root.addView(txtFalhas, lp(-1, -2, 0, 4, 0, 12));

        TextView jogoTitulo = section("JOGO DE 15 (COMPLEMENTO DAS FALHAS)");
        jogoTitulo.setTextColor(GREEN);
        root.addView(jogoTitulo);
        txtJogo = box("--", 18, GREEN);
        root.addView(txtJogo, lp(-1, -2, 0, 4, 0, 12));

        root.addView(section("CONTROLE DE REPETIDAS"));
        txtPadrao = box("O jogo projetado será obrigatoriamente ajustado para repetir 8, 9 ou 10 dezenas do último concurso do intervalo.", 13, Color.BLACK);
        root.addView(txtPadrao, lp(-1, -2, 0, 4, 0, 12));

        root.addView(section("RANKING DE TENDÊNCIA DE FALHA"));
        txtRanking = box("--", 13, Color.BLACK);
        txtRanking.setTypeface(Typeface.MONOSPACE);
        root.addView(txtRanking, lp(-1, -2, 0, 4, 0, 10));
        return page;
    }

    private TextView section(String s) {
        TextView t = label(s, 15, PURPLE_DARK, true);
        t.setPadding(0, dp(4), 0, 0);
        return t;
    }

    private TextView box(String s, int sp, int color) {
        TextView t = label(s, sp, color, true);
        t.setPadding(dp(12), dp(12), dp(12), dp(12));
        t.setBackgroundColor(Color.WHITE);
        return t;
    }

    private TextView label(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int top, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(top), dp(r), dp(b));
        return p;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private void escolherArquivo() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "text/csv", "application/octet-stream"});
        startActivityForResult(i, PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        try {
            List<Concurso> lidos = lerArquivo(uri);
            concursos.clear();
            concursos.addAll(lidos);
            if (concursos.isEmpty()) {
                txtStatus.setText("Não reconheci concursos no arquivo. Use o TXT de resultados da Lotofácil.");
                return;
            }
            txtArquivo.setText(nomeArquivo(uri));
            edtInicio.setText(String.valueOf(concursos.get(0).numero));
            edtFim.setText(String.valueOf(concursos.get(concursos.size() - 1).numero));
            txtStatus.setText(concursos.size() + " concursos encontrados. Informe o intervalo que deseja estudar.");
        } catch (Exception e) {
            txtStatus.setText("Erro ao ler arquivo: " + e.getMessage());
        }
    }

    private String nomeArquivo(Uri uri) {
        String nome = null;
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (ix >= 0) nome = c.getString(ix);
            }
        } catch (Exception ignored) {}
        return nome == null ? "Arquivo selecionado" : nome;
    }

    private List<Concurso> lerArquivo(Uri uri) throws Exception {
        Map<Integer, Set<Integer>> mapa = new LinkedHashMap<>();
        try (InputStream in = getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                Concurso c = extrairLinha(linha);
                if (c != null) mapa.put(c.numero, c.dezenas);
            }
        }
        List<Concurso> out = new ArrayList<>();
        for (Map.Entry<Integer, Set<Integer>> e : mapa.entrySet()) out.add(new Concurso(e.getKey(), e.getValue()));
        out.sort(Comparator.comparingInt(a -> a.numero));
        return out;
    }

    private Concurso extrairLinha(String linha) {
        Matcher m = NUMBER.matcher(linha);
        List<Integer> nums = new ArrayList<>();
        while (m.find()) {
            try { nums.add(Integer.parseInt(m.group())); } catch (Exception ignored) {}
        }
        if (nums.size() < 16) return null;

        int concurso = -1;
        int posConcurso = -1;
        for (int i = 0; i < nums.size(); i++) {
            if (nums.get(i) > 25) {
                concurso = nums.get(i);
                posConcurso = i;
                break;
            }
        }
        if (concurso < 1) return null;

        List<Integer> dezenas = new ArrayList<>();
        for (int i = posConcurso + 1; i < nums.size(); i++) {
            int n = nums.get(i);
            if (n >= 1 && n <= 25 && !dezenas.contains(n)) dezenas.add(n);
            if (dezenas.size() == 15) break;
        }
        if (dezenas.size() != 15) return null;
        return new Concurso(concurso, new HashSet<>(dezenas));
    }

    private void analisar() {
        if (concursos.isEmpty()) {
            toast("Escolha primeiro o arquivo de resultados.");
            return;
        }
        Integer inicio = inteiro(edtInicio.getText().toString());
        Integer fim = inteiro(edtFim.getText().toString());
        if (inicio == null || fim == null || inicio > fim) {
            toast("Informe um intervalo válido.");
            return;
        }

        List<Concurso> trecho = new ArrayList<>();
        for (Concurso c : concursos) if (c.numero >= inicio && c.numero <= fim) trecho.add(c);
        if (trecho.isEmpty() || trecho.get(0).numero != inicio || trecho.get(trecho.size() - 1).numero != fim) {
            toast("O concurso inicial e o final precisam existir no arquivo.");
            return;
        }

        txtMapa.setText(montarMapa(trecho));
        List<Set<Integer>> resultados = new ArrayList<>();
        for (Concurso c : trecho) resultados.add(c.dezenas);

        List<Score> ranking = new ArrayList<>();
        for (int d = 1; d <= 25; d++) {
            ranking.add(new Score(d, calcularScore(resultados, d), freq(resultados, d, 5), freq(resultados, d, 10), sequenciaAtual(resultados, d)));
        }
        ranking.sort((a, b) -> Double.compare(b.valor, a.valor));

        List<Integer> candidatas = new ArrayList<>();
        Map<Integer, Double> scoreMap = new HashMap<>();
        for (Score s : ranking) scoreMap.put(s.dezena, s.valor);

        // Monta uma base equilibrada para a busca: 9 dezenas que SAÍRAM no último
        // concurso + 9 dezenas que FALHARAM no último. Assim sempre existe espaço
        // para construir um jogo com 8, 9 ou 10 repetidas sem abandonar o ranking.
        Set<Integer> ultimoResultadoBase = resultados.get(resultados.size() - 1);
        int qtdSaiu = 0, qtdFalhou = 0;
        for (Score s : ranking) {
            if (ultimoResultadoBase.contains(s.dezena) && qtdSaiu < 9) {
                candidatas.add(s.dezena);
                qtdSaiu++;
            } else if (!ultimoResultadoBase.contains(s.dezena) && qtdFalhou < 9) {
                candidatas.add(s.dezena);
                qtdFalhou++;
            }
            if (qtdSaiu == 9 && qtdFalhou == 9) break;
        }

        int alvoRepetidas = alvoRepetidasDoIntervalo(trecho);
        Best best = new Best();
        combinar(candidatas, 10, 0, new ArrayList<>(), resultados, scoreMap, best, alvoRepetidas);
        if (best.combo == null) {
            toast("Não foi possível calcular o palpite.");
            return;
        }
        Collections.sort(best.combo);
        List<Integer> jogo = new ArrayList<>();
        for (int d = 1; d <= 25; d++) if (!best.combo.contains(d)) jogo.add(d);

        txtFalhas.setText(formatar(best.combo));
        txtJogo.setText(formatar(jogo));

        Set<Integer> ultimoResultado = resultados.get(resultados.size() - 1);
        int repetidasProjetadas = intersecao(new HashSet<>(jogo), ultimoResultado);
        txtPadrao.setText(
                "PADRÃO APLICADO: " + repetidasProjetadas + " repetidas do concurso " + fim +
                " (faixa obrigatória: 8, 9 ou 10).\n" +
                "Alvo estatístico do intervalo: " + alvoRepetidas + " repetidas.\n\n" +
                resumoRepeticoesHistoricas(trecho)
        );

        StringBuilder rankTxt = new StringBuilder();
        for (int i = 0; i < ranking.size(); i++) {
            Score s = ranking.get(i);
            rankTxt.append(String.format(Locale.US, "%02dº  %02d  SCORE %6.2f  F5=%3.0f%%  F10=%3.0f%%  SEQ=%d\n",
                    i + 1, s.dezena, s.valor, s.f5 * 100, s.f10 * 100, s.seq));
        }
        txtRanking.setText(rankTxt.toString());
        txtStatus.setText("Intervalo " + inicio + " a " + fim + " analisado (" + trecho.size() + " concursos). Projeção para o concurso seguinte ao " + fim + ".");
    }

    private static class Best {
        double score = Double.NEGATIVE_INFINITY;
        List<Integer> combo;
    }

    private void combinar(List<Integer> lista, int k, int inicio, List<Integer> atual,
                          List<Set<Integer>> resultados, Map<Integer, Double> scoreMap, Best best, int alvoRepetidas) {
        if (atual.size() == k) {
            // REGRA OBRIGATÓRIA DO USUÁRIO:
            // o jogo de 15 (complemento destas 10 falhas) precisa repetir
            // exatamente 8, 9 ou 10 dezenas do último concurso do intervalo.
            Set<Integer> falhasPrevistas = new HashSet<>(atual);
            Set<Integer> ultimoResultado = resultados.get(resultados.size() - 1);
            int falhasDentroDoUltimo = intersecao(falhasPrevistas, ultimoResultado);
            int repetidasNoJogo = 15 - falhasDentroDoUltimo;
            if (repetidasNoJogo < 8 || repetidasNoJogo > 10) return;

            double s = 0;
            for (int d : atual) s += scoreMap.get(d);
            s += pontuarConjunto(resultados, atual);

            // Dentro da faixa 8/9/10, prioriza o número de repetidas que mais
            // apareceu nas transições do próprio intervalo selecionado.
            if (repetidasNoJogo == alvoRepetidas) s += 12.0;
            else if (Math.abs(repetidasNoJogo - alvoRepetidas) == 1) s += 5.0;

            if (s > best.score) {
                best.score = s;
                best.combo = new ArrayList<>(atual);
            }
            return;
        }
        int faltam = k - atual.size();
        for (int i = inicio; i <= lista.size() - faltam; i++) {
            atual.add(lista.get(i));
            combinar(lista, k, i + 1, atual, resultados, scoreMap, best, alvoRepetidas);
            atual.remove(atual.size() - 1);
        }
    }

    private String montarMapa(List<Concurso> trecho) {
        StringBuilder sb = new StringBuilder("CONC  ");
        for (int d = 1; d <= 25; d++) sb.append(String.format(Locale.US, "%02d ", d));
        sb.append('\n');
        for (Concurso c : trecho) {
            sb.append(String.format(Locale.US, "%04d  ", c.numero));
            for (int d = 1; d <= 25; d++) sb.append(c.dezenas.contains(d) ? "██ " : ".. ");
            sb.append('\n');
        }
        return sb.toString();
    }

    private double calcularScore(List<Set<Integer>> r, int d) {
        double total = freq(r, d, r.size());
        double f30 = freq(r, d, 30);
        double f15 = freq(r, d, 15);
        double f10 = freq(r, d, 10);
        double f5 = freq(r, d, 5);
        int seq = sequenciaAtual(r, d);
        int maior = maiorSequencia(r, d);
        double cont = continuidade(r, d);
        double entrada = entradaFalha(r, d);
        double recente = tendenciaRecente(r, d);
        boolean ultimoFalhou = !r.get(r.size() - 1).contains(d);
        double s = total * 15 + f30 * 12 + f15 * 14 + f10 * 17 + f5 * 20 + recente * 20;
        s += (ultimoFalhou ? cont : entrada) * 22;
        if (seq == 1) s += 3; else if (seq == 2) s += 5; else if (seq == 3) s += 3; else if (seq >= 4) s -= (seq - 3) * 2.0;
        if (maior > 0 && seq >= maior) s -= 3;
        return s;
    }

    private double freq(List<Set<Integer>> r, int d, int janela) {
        if (r.isEmpty()) return 0;
        int ini = Math.max(0, r.size() - Math.min(janela, r.size()));
        int falhas = 0;
        for (int i = ini; i < r.size(); i++) if (!r.get(i).contains(d)) falhas++;
        return falhas / (double) (r.size() - ini);
    }

    private int sequenciaAtual(List<Set<Integer>> r, int d) {
        int n = 0;
        for (int i = r.size() - 1; i >= 0; i--) {
            if (!r.get(i).contains(d)) n++; else break;
        }
        return n;
    }

    private int maiorSequencia(List<Set<Integer>> r, int d) {
        int maior = 0, atual = 0;
        for (Set<Integer> x : r) {
            if (!x.contains(d)) { atual++; maior = Math.max(maior, atual); } else atual = 0;
        }
        return maior;
    }

    private double continuidade(List<Set<Integer>> r, int d) {
        int op = 0, sim = 0;
        for (int i = 0; i < r.size() - 1; i++) {
            if (!r.get(i).contains(d)) { op++; if (!r.get(i + 1).contains(d)) sim++; }
        }
        return op == 0 ? 0 : sim / (double) op;
    }

    private double entradaFalha(List<Set<Integer>> r, int d) {
        int op = 0, sim = 0;
        for (int i = 0; i < r.size() - 1; i++) {
            if (r.get(i).contains(d)) { op++; if (!r.get(i + 1).contains(d)) sim++; }
        }
        return op == 0 ? 0 : sim / (double) op;
    }

    private double tendenciaRecente(List<Set<Integer>> r, int d) {
        if (r.isEmpty()) return 0;
        double soma = 0, pesos = 0;
        for (int i = 0; i < r.size(); i++) {
            double p = i + 1;
            pesos += p;
            if (!r.get(i).contains(d)) soma += p;
        }
        return soma / pesos;
    }

    private double pontuarConjunto(List<Set<Integer>> r, List<Integer> comb) {
        Set<Integer> conjunto = new HashSet<>(comb);
        List<Set<Integer>> mapas = new ArrayList<>();
        for (Set<Integer> res : r) {
            Set<Integer> f = new HashSet<>();
            for (int d = 1; d <= 25; d++) if (!res.contains(d)) f.add(d);
            mapas.add(f);
        }
        Set<Integer> ultimo = mapas.get(mapas.size() - 1);
        int repetidasFalhas = intersecao(conjunto, ultimo);
        // Se o próximo jogo repete 8/9/10 dezenas do último resultado,
        // as 10 falhas previstas repetem 3/4/5 falhas do último concurso.
        double s;
        if (repetidasFalhas == 4) s = 24;
        else if (repetidasFalhas == 3 || repetidasFalhas == 5) s = 20;
        else s = -1000;

        int ini = Math.max(0, mapas.size() - 15);
        int total = mapas.size() - ini;
        for (int i = ini; i < mapas.size(); i++) {
            int inter = intersecao(conjunto, mapas.get(i));
            double peso = (i - ini + 1) / (double) total;
            if (inter == 5) s += 5 * peso; else if (inter == 4 || inter == 6) s += 3 * peso; else if (inter == 3 || inter == 7) s += peso;
        }

        int[][] grupos = {
                {1,2,3,4,5},{6,7,8,9,10},{11,12,13,14,15},{16,17,18,19,20},{21,22,23,24,25},
                {1,6,11,16,21},{2,7,12,17,22},{3,8,13,18,23},{4,9,14,19,24},{5,10,15,20,25}
        };
        for (int[] g : grupos) {
            int q = 0;
            for (int d : g) if (conjunto.contains(d)) q++;
            if (q >= 4) s -= 5; else if (q == 3) s -= 1;
        }
        return s;
    }

    private int alvoRepetidasDoIntervalo(List<Concurso> trecho) {
        int[] cont = new int[11];
        for (int i = 1; i < trecho.size(); i++) {
            int rep = intersecao(trecho.get(i - 1).dezenas, trecho.get(i).dezenas);
            if (rep >= 8 && rep <= 10) cont[rep]++;
        }
        int alvo = 9;
        int melhor = -1;
        // Em empate, prefere 9; depois 10; depois 8.
        int[] ordem = {9, 10, 8};
        for (int r : ordem) {
            if (cont[r] > melhor) {
                melhor = cont[r];
                alvo = r;
            }
        }
        return alvo;
    }

    private String resumoRepeticoesHistoricas(List<Concurso> trecho) {
        if (trecho.size() < 2) return "Intervalo curto: não há transições suficientes para comparar repetidas.";
        StringBuilder sb = new StringBuilder("Repetidas observadas dentro do intervalo:\n");
        int dentro = 0;
        int total = 0;
        for (int i = 1; i < trecho.size(); i++) {
            Concurso ant = trecho.get(i - 1);
            Concurso atual = trecho.get(i);
            int rep = intersecao(ant.dezenas, atual.dezenas);
            boolean ok = rep >= 8 && rep <= 10;
            if (ok) dentro++;
            total++;
            sb.append(atual.numero).append(" x ").append(ant.numero)
              .append(": ").append(rep).append(" repetidas")
              .append(ok ? "  ✓ padrão" : "  • fora de 8-10")
              .append('\n');
        }
        sb.append("Dentro do padrão 8-10: ").append(dentro).append(" de ").append(total).append(" transições.");
        return sb.toString();
    }

    private int intersecao(Set<Integer> a, Set<Integer> b) {
        int n = 0;
        for (int x : a) if (b.contains(x)) n++;
        return n;
    }

    private String formatar(List<Integer> dezenas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dezenas.size(); i++) {
            if (i > 0) sb.append("  ");
            sb.append(String.format(Locale.US, "%02d", dezenas.get(i)));
        }
        return sb.toString();
    }

    private Integer inteiro(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private void toast(String s) {
        txtStatus.setText(s);
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
