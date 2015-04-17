package ost.iuexternalresults;

/**
 * Транслятор внешних результатов соревнований по программированию на базе проверяющей системы Фёдора Владимировича Меньшикова.
 * Параметры запуска: <адрес до сайта соревнования> <префикс для страниц результатов> <директория для выгрузки результатов>
 * <p/>
 * Author: Oleg Strekalovsky
 * Date: 17.04.2015
 */
public class Main {

    public static void main(String[] args) {
        String baseUrl = args[0];
        String resultPagePrefix = args[1];
        String targetDir = args[2];
        try {
            new Worker(baseUrl, resultPagePrefix).run(targetDir, resultPagePrefix);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
